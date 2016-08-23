package encap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class SccCallGraphNode extends AbstractCallGraphNode {

	private List<AbstractCallGraphNode> scc = null;
	
	public SccCallGraphNode(List<AbstractCallGraphNode> scc) {
		this.scc = scc;
	}
	
	public List<AbstractCallGraphNode> getScc() {
		return scc;
	}

	@Override
	void analyze() {
		CommonUtils.println("=== Analyzing SCC call graph node [" + toSimpleStr() + "]... ===");
		// find the methods that have been analyzed or whose callers have been analyzed in the SCC
		int firstAnalyzed = -1;
		boolean isOneAnalyzed = false;
		
		for (int i = 0; i < scc.size(); ++i) {
			SimpleCallGraphNode node = (SimpleCallGraphNode) scc.get(i);
			MethodDeclaration mtd = node.getMtd();
			TypeDeclaration currTpd = CommonUtils.getDeclaringTpd(mtd);
			ClassCG ccg = ClassCG.tpd2ccg.get(currTpd);
			if (ccg == null)
				continue;
			MethodCG mcg = ccg.getMethodCGOfCurrClass(mtd);
			
			if (mcg != null) {
				firstAnalyzed = i;
				isOneAnalyzed = true;
				break;
			}
		}
		
		int calleeFirstAnalyzed = -1;
		boolean isOneCalleeAnalyzed = false;
		
		for (int i = 0; i < scc.size(); ++i) {
			SimpleCallGraphNode node = (SimpleCallGraphNode) scc.get(i);
			Set<AbstractCallGraphNode> callees = node.getCallees();
			for (AbstractCallGraphNode cgn : callees) {
				if (cgn instanceof SimpleCallGraphNode) {
					MethodDeclaration mtd = ((SimpleCallGraphNode)cgn).getMtd();
					TypeDeclaration currTpd = CommonUtils.getDeclaringTpd(mtd);
					ClassCG ccg = ClassCG.tpd2ccg.get(currTpd);
					if (ccg == null)
						continue;
					MethodCG mcg = ccg.getMethodCGOfCurrClass(mtd);
					
					if (mcg != null) {
						calleeFirstAnalyzed = i;
						isOneCalleeAnalyzed = true;
						break;
					}
				}
			}
		}

		// sorting on the copy
		List<AbstractCallGraphNode> scc2 = new ArrayList<AbstractCallGraphNode>(scc);
		if (isOneAnalyzed) {
			// rotate & reverse
			// 0 0 0 1 0 0 => 0 0 0 0 0 1
			// 0 0 0 0 0 1 => 1 0 0 0 0 0
			Collections.rotate(scc2, scc2.size()-firstAnalyzed-1);
			Collections.reverse(scc2);
		} else if (isOneCalleeAnalyzed) {
			Collections.rotate(scc2, scc2.size()-calleeFirstAnalyzed-1);
			Collections.reverse(scc2);
		} else {
			// do nothing
			// a random one is chosen
		}
		
		// perform an iterative analysis until a fixed point or threshold is reached
		boolean modified = true;
		// threshold
		int iterStep = 10;
		while (modified && iterStep > 0) {
			List<MethodCG> cgsBefore = new ArrayList<>();
			
			for (AbstractCallGraphNode cgn : scc2) {
				MethodDeclaration mtd = ((SimpleCallGraphNode)cgn).getMtd();
				TypeDeclaration currTpd = CommonUtils.getDeclaringTpd(mtd);
				ClassCG ccg = ClassCG.tpd2ccg.get(currTpd);
				
				if (ccg == null) {
					cgsBefore.add(null);
					continue;
				}
				
				MethodCG mcg = ccg.getMethodCGOfCurrClass(mtd);
				cgsBefore.add(mcg);
			}
			
			for (AbstractCallGraphNode cgn : scc2)
				cgn.analyze();
			
			modified = false;
			
			for (int i = 0; i < scc2.size(); ++i) {
				MethodDeclaration mtd = ((SimpleCallGraphNode)scc2.get(i)).getMtd();
				
				// ommit the cases in which the method declaration is in a eum
				TypeDeclaration currTpd = CommonUtils.getDeclaringTpd(mtd);
				ClassCG ccg = ClassCG.tpd2ccg.get(currTpd);
				
				if (ccg == null)
					continue;
				
				MethodCG mcgNew = ccg.getMethodCGOfCurrClass(mtd);
				MethodCG mcgOld = cgsBefore.get(i);
				
				if (!mcgNew.equals(mcgOld)) {
					modified = true;
					break;
				}
			}
			--iterStep;
		}
	}
	
	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SCC: ");
		sb.append(toSimpleStr());
		sb.append("\n");
		
		sb.append("Callers: \n");
		for (AbstractCallGraphNode cgn : getCallers())
			sb.append("\t").append(cgn.toSimpleStr()).append("\n");
		
		sb.append("Callees: \n");
		for (AbstractCallGraphNode cgn : getCallees())
			sb.append("\t").append(cgn.toSimpleStr()).append("\n");
		
		return sb.toString();
	}

	@Override
	public String toSimpleStr() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < scc.size(); ++i) {
			SimpleCallGraphNode scgn = (SimpleCallGraphNode) scc.get(i);
			TypeDeclaration tpd = CommonUtils.getDeclaringTpd(scgn.getMtd());
			sb.append(CommonUtils.getMethodSignature(scgn.getMtd()));
			sb.append("[");
			sb.append(CommonUtils.getTpdName(tpd));
			sb.append("]");
			if (i != scc.size()-1)
				sb.append(" --> ");
		}
		return sb.toString();
	}
}
