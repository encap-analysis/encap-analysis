package encap;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class SimpleCallGraphNode extends AbstractCallGraphNode {
	protected MethodDeclaration mtd = null;
	
	public static SimpleCallGraphNode SCC_BEGIN = new SimpleCallGraphNode();
	public static SimpleCallGraphNode SCC_END = new SimpleCallGraphNode();
	
	// only for internal tag uses
	private SimpleCallGraphNode() {
		
	}
	
	@ Override
	void analyze() {
		CommonUtils.println("=== Analyzing simple call graph node [" + toSimpleStr() + "]... ===");
		
		// Omit the methods with no bodies or declared in anonymous classes
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
		
		if (currTpd.getName().toString().equals("AbstractStringBuilder"))
			CommonUtils.debug();
		
		// mcg should be be changed here
		MethodCG mcg = new MethodCG(mtd, ccg.getClassCG());
		ccg.addMtd2Mcg(mtd, mcg);
	}

	SimpleCallGraphNode(MethodDeclaration method) {
		mtd = method;
	}
	
	SimpleCallGraphNode(SimpleCallGraphNode cgn) {
		this.mtd = cgn.mtd;
	}
	
	boolean correspondTo(MethodDeclaration md) {
		if (mtd == null)
			return false;
		if (md != null)
			if (mtd.getStartPosition() == md.getStartPosition() && mtd.getLength() == md.getLength())
				return true;
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		SimpleCallGraphNode other = (SimpleCallGraphNode) obj;

		return correspondTo(other.mtd);
	}
	
	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Simple: " + toSimpleStr() + "\n");
		
		sb.append("Callers: \n");
		for (AbstractCallGraphNode cgn : getCallers())
			sb.append("\t").append(cgn.toSimpleStr()).append("\n");
		
		sb.append("Callees: \n");
		for (AbstractCallGraphNode cgn : getCallees())
			sb.append("\t").append(cgn.toSimpleStr()).append("\n");
		
		return sb.toString();
	}
	
	@ Override
	public String toSimpleStr() {
		TypeDeclaration tpd = CommonUtils.getDeclaringTpd(mtd);
		return CommonUtils.getMethodSignature(mtd) + "[" + CommonUtils.getTpdName(tpd) + "]";
	}

	public MethodDeclaration getMtd() {
		return mtd;
	}

}
