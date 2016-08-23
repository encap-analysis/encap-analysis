package encap;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.nju.cs.DataFlowAnalysis.FrameWork.ForwardAnalysisAlgorithm;
import edu.nju.cs.ui.Demo;
import edu.nju.cs.util.MethodUtils;

/**
 * CG of methods
 * @author Tom
 *
 */
public class MethodCG {	
	private MethodDeclaration mtd = null;	// correspondign method declaration
	
	private Integer exitPPT = null;
	
	// The CG at the exit point of the method
	private EncapDFV exitDFV = null;
	private Set<CGNode> retNodes = CommonUtils.newHashSet();
	
	private ForwardAnalysisAlgorithm faa = null;
	
	private ConnectionGraph classCG = null;
	
	public MethodCG(MethodDeclaration mtd, ConnectionGraph classCG) {
		super();
		this.mtd = mtd;
		this.classCG = classCG;
		CommonUtils.println("=== Analyzing method " + CommonUtils.getMethodSignature(mtd) + " of class " + CommonUtils.getTpdName(CommonUtils.getDeclaringTpd(mtd)) + " ===");
		
		if (CommonUtils.getMethodSignature(mtd).equals("random(int min, int max)")) {
			CommonUtils.debug();
		}
		
		buildCG();
		//toNeo4jDb();
	}
	
	/**
	 * Build the CG of method
	 * Perform dataflow analysis
	 * Perform reachability analysis on the CG at the end of the method, then update EscapeState.
	 */
	void buildCG() {
		// Ommit methods with no bodies or declared in anonymous classes
		if (mtd.getBody() == null || CommonUtils.isDeclaredInAcd(mtd))
			return;
		
		EncapDFV initDFV = new EncapDFV();
		ConnectionGraph initCG = classCG.clone();
		initDFV.setCG(initCG);
		
		List<SingleVariableDeclaration> params = mtd.parameters();
		for (SingleVariableDeclaration ai : params) {			
			// only consider parameters of non-primitive types
			if (!ai.getType().isPrimitiveType()) {
				ParamVarNode node = initCG.addParamVarNode(ai.getName());
				ObjectNode phantom = initCG.addJavaObjectNode(ai);
				initCG.addPointsToEdge(node, phantom);
			}
		}
		
//		TypeDeclaration declaringTPD = Utils.getDeclaringTpd(mtd);
//		if (declaringTPD.resolveBinding().isAnonymous())
//			return;
		
		faa = new ForwardAnalysisAlgorithm(Demo.project, mtd, initDFV);
		//faa.TwoPhaseExecution();
		
		exitPPT = Demo.project.getMethodExit(MethodUtils.getMethodIdStr(mtd));
		if (exitPPT == null)
			return;
		exitDFV = (EncapDFV) faa.getDFVs().get(exitPPT);
		
		// deal with return values
		List<Integer> ppts = Demo.project.getPreds(exitPPT);
		if (ppts != null)
			for (int ppt : ppts) {
				Statement st = Demo.project.getStBeforePPT(ppt);
				if (st instanceof ReturnStatement) {
					Expression returnExp = ((ReturnStatement)st).getExpression();
					Set<CGNode> rets = EncapDFVUtils.handle(exitDFV, returnExp);
					if (rets == null) {
						continue;
					}
					for (CGNode node : rets) {
						if (node == null)
							continue;
						// return this
						if (node instanceof ObjectNode) {
							ObjectNode obj = (ObjectNode)node;
							if (obj.getASTNode() instanceof TypeDeclaration)
								continue;
						}
						node.adjustEscapeState(EscapeState.RetEscape);
					}

					retNodes.addAll(rets);
				}
			}
		
		exitDFV.getCG().updateEscapeState();
//		Utils.println("Printing CG of method <" + Utils.getMethodSignature(mtd) + "> ...");
//		Utils.println("**************** Begin ****************");
//		Utils.print(exitDFV.getCG().toString());
//		Utils.println("****************  End  ****************");
	}
	
	ConnectionGraph getExitCG() {
		if (exitDFV == null) {
			//Utils.printlnError("Error!");
			return null;
		}
		return exitDFV.getCG();
	}
	
	Set<CGNode> getRetNodes() {
		return retNodes;
	}
	
	MethodDeclaration getMtd() {
		return mtd;
	}
	
	@ Override
	public String toString() {
		if (exitDFV == null)
			return "Invalid CG";
		ConnectionGraph cg = exitDFV.getCG();
		return cg == null ? "Invalid CG." : cg.toString();
	}
	
	public void toNeo4jDb() {
		Neo4jService.getInstance().store(this);
		CommonUtils.println("toNeo4jDb succeed!");
	}
	
	@ Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof MethodCG))
			return false;
		
		MethodCG othermcg = (MethodCG) other;
		
		// can use pointer equality instead of equals, for simplicity
		if (mtd != othermcg.mtd)
			return false;
		
		if (exitPPT != othermcg.exitPPT)
			return false;
		
		if (classCG != othermcg.classCG)
			return false;
		
		return exitDFV.equals(othermcg.exitDFV);
	}

}
