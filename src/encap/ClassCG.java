package encap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;


/**
 * CG of class
 * @author Tom
 *
 */
public class ClassCG {
	public static Map<TypeDeclaration, ClassCG> tpd2ccg = CommonUtils.newHashMap();
	public static Map<TypeDeclaration, ObjectNode> tpd2obj = CommonUtils.newHashMap();
	
	private ObjectNode objNode = null;
	
	static ObjectNode currObjNode = null;
	static TypeDeclaration currTpd = null;
	
	private TypeDeclaration tpd = null;		
	private TypeDeclaration superClass = null;
	private TypeDeclaration outerClass = null;
	private ConnectionGraph cg = new ConnectionGraph();
	
	private Map<MethodDeclaration, MethodCG> mtd2mcg = CommonUtils.newHashMap();
	
	Map<SimpleName, Map<MethodDeclaration, EscapeState>> escapeRecord = CommonUtils.newHashMap();
	
	int argEscaped = 0;
	int retEscaped = 0;
	int gloEscaped = 0;
	
	static int encapClassCount = 0;
	static int notEncapClassCount = 0;
	static int argClassCount = 0;
	static int retClassCount = 0;
	static int gloClassCount = 0;
	
	
	public static ClassCG createClassCG(TypeDeclaration tpd, boolean bHandleMethods) {
		return new ClassCG(tpd, bHandleMethods);
	}
	
	private ClassCG(TypeDeclaration tpd, boolean bHandleMethods) {
		super();
		tpd2ccg.put(tpd, this);
		this.tpd = tpd;
		currTpd = tpd;
		
		String tpdName = CommonUtils.getTpdName(tpd);
		String superClassName = Demo.project.getSuperClassName(tpdName);
		if (superClassName != null) {
			ASTNode node = Demo.project.getClassByName(superClassName);
			if (node != null)
				superClass = (TypeDeclaration) node;
		}
		
		String outerClassName = Demo.project.getOuterClassName(tpdName);
		if (outerClassName != null) {
			ASTNode node = Demo.project.getClassByName(outerClassName);
			if (node != null)
				outerClass = (TypeDeclaration) node;
		}
		
		buildCG(bHandleMethods);
	}
	
	void buildCG(boolean bHandleMethods) {
		buildInitCG();
		if (bHandleMethods) {
			buildMethodCGs();
			evaluate();
		}
		
//		Utils.println("Analysis of class <" + tpd.getName() + "> finished.");
//		Utils.println("Prining CG of class <" + tpd.getName() + "> ...");
//		Utils.println("**************** Begin ****************");
//		Utils.print(cg.toString());
//		Utils.println("****************  End  ****************");
	}
	
	void buildInitCG() {
		ClassCG.currObjNode = objNode = initFieldNodes();
		objNode.setSuperObj(getSuperClassObj());
		tpd2obj.put(tpd, ClassCG.currObjNode);
	}
	
	@SuppressWarnings("null")
	private ObjectNode initFieldNodes() {
		TypeDeclaration tpd = this.tpd;
		
		ObjectNode objNode = cg.addJavaObjectNode(tpd);
		
		FieldDeclaration[] flds = tpd.getFields();
		for (FieldDeclaration fld : flds) {
			for (VariableDeclarationFragment vdf : (List<VariableDeclarationFragment>)fld.fragments()) {
				SimpleName fieldName = vdf.getName();
				if (fieldName == null) {
					CommonUtils.printlnError("Error: field " + fieldName.getIdentifier() + " of " + tpd.getName() + " is invalid!");
				}
				
				if (CommonUtils.isPublic(fieldName))
					++gloEscaped;
				
				if (CommonUtils.isPrimitive(fieldName) == null || CommonUtils.isPrimitive(fieldName))
					continue;
				
				CGNode fieldNode = objNode.addFieldNode(fieldName);
				if (CommonUtils.isStatic(fieldName) || CommonUtils.isPublic(fieldName))
					fieldNode.adjustEscapeState(EscapeState.GlobalEscape);
				
				if (true) {
					Expression initializer = vdf.getInitializer();
					// T p;
					// T p = null;
					if (initializer == null || initializer instanceof NullLiteral)
						continue;
					// T p = new T;
					if (initializer instanceof ClassInstanceCreation) {
						TypeDeclaration otherTPD = CommonUtils.getTyd((ClassInstanceCreation)initializer);
						ObjectNode node = null;
						if (objNode.correspondTo(otherTPD))
							node = cg.addJavaObjectNode(initializer);
							//node = new ObjectNode((ClassInstanceCreation)initializer, false);
						else
							node = cg.addJavaObjectNode(initializer);
							//node = new ObjectNode((ClassInstanceCreation)initializer, true);
						
						cg.addFieldConnection(objNode, node, fieldName);
					}
					// T p = f();
					else if (initializer instanceof MethodInvocation) {
						// TODO
					}
				}
			}
		}		
		
		return objNode;
	}
	
	ObjectNode getSuperClassObj() {
		ClassCG superClassCG = ClassCG.tpd2ccg.get(superClass);
		if (superClassCG == null) {
			//Utils.debug("super class CG not found.");
			return null;
		}
		
		return superClassCG.objNode;
	}

	void buildMethodCGs() {
		MethodDeclaration[] mtds = tpd.getMethods();
		for (MethodDeclaration mtd : mtds) {
			MethodCG mcg = new MethodCG(mtd, cg);
			if (mtd.getBody() != null)
				mtd2mcg.put(mtd, mcg);
		}
	}
	
	public void addMtd2Mcg(MethodDeclaration mtd, MethodCG mcg) {
		mtd2mcg.put(mtd, mcg);
	}
	
	Set<SimpleName> getAllFields() {
		Set<SimpleName> fields = CommonUtils.newHashSet();
		FieldDeclaration[] flds = tpd.getFields();
		for (FieldDeclaration fld : flds) {
			for (VariableDeclarationFragment vdf : (List<VariableDeclarationFragment>)fld.fragments()) {
				SimpleName fldName = vdf.getName();
				if (CommonUtils.isPrimitive(fldName) != null && !CommonUtils.isPrimitive(fldName) && CommonUtils.isStatic(fldName) != null && !CommonUtils.isStatic(fldName) && CommonUtils.isPublic(fldName) != null && !CommonUtils.isPublic(fldName))
					fields.add(vdf.getName());
			}
		}
		
		return fields;
	}
	
	public void evaluate() {
		CommonUtils.println("=== Evaluting class " + tpd.getName()+ " in package " + Demo.project.getPackageOf(currTpd).getName() + " ===");
		
		if (tpd.getName().toString().equals("AbstractStringBuilder"))
			CommonUtils.debug();
		
		for (Entry<MethodDeclaration, MethodCG> entry : mtd2mcg.entrySet()) {
			MethodDeclaration mtd = entry.getKey();
			MethodCG mcg = entry.getValue();
			CommonUtils.println(">>> Evaluating method " + CommonUtils.getMethodSignature(mtd));
			evaluateMethod(mtd, mcg);
		}
		
		Set<SimpleName> fields = getAllFields();
		fields.removeAll(escapeRecord.keySet());
		
		if (!fields.isEmpty()) {
			CommonUtils.println("*** Encapsulated fields of " + tpd.getName() + ": " + fields);
		}
		
		if (!escapeRecord.isEmpty()) {
			CommonUtils.println("*** Escaped fields: ");
			StringBuilder sb = new StringBuilder();
			for (Entry<SimpleName, Map<MethodDeclaration, EscapeState>> entry : escapeRecord.entrySet()) {
				Map<MethodDeclaration, EscapeState> escapeRecords = entry.getValue();
				if (escapeRecords != null && !escapeRecords.isEmpty()) {
					sb.append("    ");
					sb.append(entry.getKey().toString());
					sb.append(": ");
					for (Entry<MethodDeclaration, EscapeState> e : escapeRecords.entrySet()) {
						sb.append(CommonUtils.getMethodSignature(e.getKey()));
						sb.append("[");
						sb.append(e.getValue());
						sb.append("], ");
					}
					sb.append("\n");
				}
			}
			CommonUtils.print(sb.toString());
			++notEncapClassCount;
		} else {
			++encapClassCount;
		}
		
		if (argEscaped > 0)
			++argClassCount;
		if (retEscaped > 0)
			++retClassCount;
		if (gloEscaped > 0)
			++gloClassCount;
		
		CommonUtils.println("------  End of class " + tpd.getName() + "   ------\n");		
	}
	
	static ObjectNode getThisObject(ConnectionGraph cg) {
		return (ObjectNode) cg.getCGNodeByAST(currObjNode.getASTNode(), NodeType.JavaObject);
	}
	
	void evaluateMethod(MethodDeclaration mtd, MethodCG mcg) {		
		ConnectionGraph theCG = mcg.getExitCG();
		if (theCG == null) {
			CommonUtils.debug("exit CG of method " + CommonUtils.getMethodSignature(mtd) + " not available!");
			return;
		}
		ObjectNode thisObj = getThisObject(theCG);
		if (thisObj == null)
			CommonUtils.println("No non-static fields of class " + tpd.resolveBinding().getQualifiedName() + " are escaped in method " + CommonUtils.getMethodSignature(mtd));
		else 
			report(mtd, thisObj);
	}
	
	void markAsEscape(FieldNode fld, MethodDeclaration mtd, EscapeState es) {
		Map<MethodDeclaration, EscapeState> record;
		record = escapeRecord.get((SimpleName)fld.getASTNode());
		if (record == null) {
			record = new HashMap<>();
			escapeRecord.put((SimpleName)fld.getASTNode(), record);
		}
		record.put(mtd, es);
	}
	
	void report(MethodDeclaration mtd, ObjectNode thisObj) {		
		boolean escaped = false;
		
		// report fieldnode
		for (FieldNode fld: thisObj.getFieldNodes()) {
			switch (fld.getEscapeState()) {
			case ArgEscape:
				CommonUtils.println("    !!!ArgEscape: " + fld.toSimpleString());
				markAsEscape(fld, mtd, EscapeState.ArgEscape);
				escaped = true;
				++argEscaped;
				break;
			case RetEscape:
				CommonUtils.println("    !!!RetEscape: " + fld.toSimpleString());
				markAsEscape(fld, mtd, EscapeState.RetEscape);
				escaped = true;
				++retEscaped;
				break;
			default:
				break;
			}
			
			// report objects pointed to by fieldnode
			Set<ObjectNode> ptNodes = fld.getPointsToNode();
			if (ptNodes != null && !ptNodes.isEmpty()) {
				for (ObjectNode obj : ptNodes) {
					switch (obj.getEscapeState()) {
					case ArgEscape:
						CommonUtils.println("    !!!ArgEscape: " + obj.toSimpleString() + " pointed to by " + fld.toSimpleString());
						markAsEscape(fld, mtd, EscapeState.ArgEscape);
						escaped = true;
						++argEscaped;
						break;
					case RetEscape:
						CommonUtils.println("    !!!RetEscape: " + obj.toSimpleString() + " pointed to by " + fld.toSimpleString());
						markAsEscape(fld, mtd, EscapeState.RetEscape);
						escaped = true;
						++retEscaped;
						break;
					default:
						break;
					}
				}
			}
			else {
				Set<RefNode> directRefNodes = fld.getDirectRefersToNode();
				for (RefNode refNode : directRefNodes) {
					switch (refNode.getEscapeState()) {
					case ArgEscape:
						CommonUtils.println("    !!!ArgEscape: " + refNode.toSimpleString() + " pointed to by " + fld.toSimpleString());
						markAsEscape(fld, mtd, EscapeState.ArgEscape);
						escaped = true;
						++argEscaped;
						break;
					case RetEscape:
						CommonUtils.println("    !!!RetEscape: " + refNode.toSimpleString() + " pointed to by " + fld.toSimpleString());
						markAsEscape(fld, mtd, EscapeState.RetEscape);
						escaped = true;
						++retEscaped;
						break;
					default:
						break;
					}
				}
			}
		}
		
		if (escaped) {
			String mtdSig = CommonUtils.getMethodSignature(mtd);
			if (mtdSig.equals("getNormalizedCalendar()") || mtdSig.equals("getZone()") || mtdSig.equals("getActualMinimum(int field)")) {
				return;
			}
			CallGraph callgraph = Demo.project.getCallGraph();
			List<List<AbstractCallGraphNode>> callTraces = callgraph.getCallTraces(mtd);
			for (List<AbstractCallGraphNode> trace : callTraces) {
				GraphUtils.printCallTrace(trace);
			}
		} else {
			CommonUtils.println("    No escapement detected.");
		}
	}
	
	void updateEscapeState() {
		cg.updateEscapeState();
	}

	@ Override
	public String toString() {
		return cg.toString();
	}
	
	public ConnectionGraph getClassCG() {
		return cg;
	}
	
	MethodCG getMethodCGOfCurrClass(MethodDeclaration mtd) {
		return mtd2mcg.get(mtd);
	}
	
	static MethodCG getMethodCG(MethodDeclaration mtd) {
		ASTNode astnode = mtd.getParent();
		if (!(astnode instanceof TypeDeclaration))
			return null;
		
		TypeDeclaration tpd = (TypeDeclaration)astnode;
		ClassCG classCG = tpd2ccg.get(tpd);
		if (classCG == null)
			return null;
		
		return classCG.mtd2mcg.get(mtd);
	}
	
	public static ObjectNode getCurrObjNode() {
		return currObjNode;
	}
	
	public static void setCurrObjNode(ObjectNode obj) {
		currObjNode = obj;
	}
	
	public static void setCurrTpd(TypeDeclaration tpd) {
		currTpd = tpd;
	}
	
	public void export2Neo4J() {
		
	}
	
}