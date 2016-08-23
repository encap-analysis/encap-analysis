package encap;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class InheritTree {

	protected Set<InheritTreeNode> nodes = CommonUtils.newHashSet();
	
	private boolean isInheritTreeInitialized = false;
	
	public static InheritTree getInheritTree4Project(Project project) {
		InheritTree inheritTree = new InheritTree();
		inheritTree.build4Project(project);
		return inheritTree;
	}

	public InheritTree() {
		// do nothing
	}

	public void build4Project(Project project) {
		nodes.clear();
		
		Hashtable<String, ASTNode> name2Class = Demo.project.getNameToClass();
		for (ASTNode astnode : name2Class.values()) {
			if (astnode instanceof TypeDeclaration) {
				TypeDeclaration tpd = (TypeDeclaration) astnode;				
				InheritTreeNode itNode = new InheritTreeNode(tpd);
				nodes.add(itNode);
			}
		}
		for (Entry<String, ASTNode> entry : name2Class.entrySet()) {
			ASTNode astnode = entry.getValue();
			if (astnode instanceof TypeDeclaration) {
				String tpdName = entry.getKey();
				TypeDeclaration tpd = (TypeDeclaration) entry.getValue();
				
				InheritTreeNode itn = findNodeByTpd(tpd);
				
				String superClassName = Demo.project.getSuperClassName(tpdName);
				if (superClassName != null) {
					ASTNode node = Demo.project.getClassByName(superClassName);
					if (node != null) {
						TypeDeclaration superClass = (TypeDeclaration) node;
						if (itn.superClass == null)
							itn.superClass = findNodeByTpd(superClass);
						else
							CommonUtils.debug("super class already exists!");
					}
				}
				
				List<String> subClassNames = Demo.project.getSubClassNames(tpdName);
				if (subClassNames != null) {
					for (String subClassName : subClassNames) {
						ASTNode node = Demo.project.getClassByName(subClassName);
						if (node != null && node instanceof TypeDeclaration) {
							TypeDeclaration subClass = (TypeDeclaration) node;
							itn.subClasses.add(findNodeByTpd(subClass));
						}
					}
				}
			}
		}
	}
	
	InheritTreeNode findNodeByTpd(TypeDeclaration tpd) {
		for (InheritTreeNode itn: nodes) {
			if (itn.correspondTo(tpd))
				return itn;
		}
		return null;
	}
	
	public void analyze(SimpleCallGraphNode cgn) {
		MethodDeclaration mtd = cgn.getMtd();
		
	
		// Ommit methods with no bodies or declared in anonymous classes
		if (mtd.getBody() == null || CommonUtils.isDeclaredInAcd(mtd)) {
			CommonUtils.debug("Method delclaration " + CommonUtils.getMethodSignature(mtd) + " is skipped.");
			return;
		}
		
		TypeDeclaration currTpd = CommonUtils.getDeclaringTpd(mtd);
		if (currTpd == null) {
			CommonUtils.debug("Type declaration of " + CommonUtils.getMethodSignature(mtd) + " not found!");
			return;
		}
		
		ObjectNode currObjNode = ClassCG.tpd2obj.get(currTpd);
		if (currObjNode == null) {
			CommonUtils.debug("Should not be here!");
			return;
		}
		ClassCG.setCurrTpd(currTpd);
		ClassCG.setCurrObjNode(currObjNode);

		ClassCG ccg = ClassCG.tpd2ccg.get(currTpd);
		MethodCG mcg = new MethodCG(mtd, ccg.getClassCG());
		ccg.addMtd2Mcg(mtd, mcg);
		CommonUtils.println(mcg.toString());
	}
	
	public void initClassCGs() {
		Set<InheritTreeNode> roots = getRootNodes();
		
		for (InheritTreeNode root : roots) {
			init(root);
		}
	}
	
	// DFS inorder traversal
	private void init(InheritTreeNode itn) {
		ClassCG.createClassCG(itn.tpd, false);
		
		for (InheritTreeNode child : itn.subClasses) {
			init(child);
		}
	}
	
	private Set<InheritTreeNode> getRootNodes() {
		Set<InheritTreeNode> roots = new HashSet<>();
		for (InheritTreeNode itn : nodes) {
			if (itn.superClass == null)
				roots.add(itn);
		}
		return roots;
	}
}