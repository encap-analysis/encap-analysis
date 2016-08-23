package encap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.TypeDeclaration;


public class GraphUtils {

	static List<AbstractCallGraphNode> topsort(CallDag calldag) {
		List<AbstractCallGraphNode> l = CommonUtils.newArrayList();
		Deque<AbstractCallGraphNode> s = CommonUtils.newArrayDeque();
		for (AbstractCallGraphNode cgn : calldag.getDagNodes()) {
			if (cgn.callers.isEmpty())
				s.push(cgn);
		}
		
		while (!s.isEmpty()) {
			AbstractCallGraphNode n = s.pop();
			l.add(n);
			Iterator<AbstractCallGraphNode> iter = n.getCallees().iterator();
			while (iter.hasNext()) {
				AbstractCallGraphNode m = iter.next();
				iter.remove();
				m.callers.remove(n);
				if (m.callers.isEmpty())
					s.push(m);
			}
		}
		
//		for (AbstractCallGraphNode cgn : calldag.getDagNodes()) {
//			if (!cgn.callees.isEmpty() || !cgn.callers.isEmpty())
//				return null;
//		}

		return l;
	}

	static List<AbstractCallGraphNode> getUniqueCallersOfScc(List<AbstractCallGraphNode> scc) {
		List<AbstractCallGraphNode> callers = CommonUtils.newArrayList();
		for (AbstractCallGraphNode cgn : scc) {
			if (cgn != null) {
				for (AbstractCallGraphNode caller : cgn.getCallers()) {
					if (scc.indexOf(caller) == -1 && callers.indexOf(caller) == -1) {
						callers.add(caller);
					}
				}
			}
		}
		return callers;
	}

	static List<AbstractCallGraphNode> getUniqueCalleesOfScc(List<AbstractCallGraphNode> scc) {
		List<AbstractCallGraphNode> callees = CommonUtils.newArrayList();
		for (AbstractCallGraphNode cgn : scc) {
			if (cgn != null) {
				for (AbstractCallGraphNode caller : cgn.getCallees()) {
					if (scc.indexOf(caller) == -1 && callees.indexOf(caller) == -1) {
						callees.add(caller);
					}
				}
			}
		}
		return callees;
	}

	public static void printCallTrace(List<AbstractCallGraphNode> trace) {
		StringBuilder sb = new StringBuilder();
		sb.append("    Call trace: ");
		for (int i = 0; i < trace.size(); ++i) {
			SimpleCallGraphNode cgn = (SimpleCallGraphNode) trace.get(i);
			if (cgn == SimpleCallGraphNode.SCC_BEGIN) {
				sb.append("|>");
			} else if (cgn == SimpleCallGraphNode.SCC_END) {
				sb.append("<|");
			} else {
				if (i != 0)
					sb.append("-> ");
				sb.append(CommonUtils.getMethodSignature(cgn.getMtd()));
				if (i != trace.size()-1)
					sb.append(" -");
			}
		}
		CommonUtils.println(sb.toString());
	}
	
	static AbstractCallGraphNode getFirstCalleeInScc(AbstractCallGraphNode caller, List<AbstractCallGraphNode> scc) {
		for (AbstractCallGraphNode callee : scc) {
			if (caller.callees.contains(callee)) {
				return callee;
			}
		}
		CommonUtils.debug("no callee found in cycle.");
		return null;
	}
	
	static List<AbstractCallGraphNode> getProperCallTraceOrder(AbstractCallGraphNode caller, List<AbstractCallGraphNode> scc) {
		AbstractCallGraphNode firstCallee = getFirstCalleeInScc(caller, scc);
		int index = scc.indexOf(firstCallee);
		List<AbstractCallGraphNode> trace = new ArrayList<>(scc);
		Collections.rotate(trace, -index);
		trace.add(firstCallee);
		return trace;
	}
	
	
	static void printTypesInPackges() {
		CommonUtils.println("=== Printing types and packages... ===\n");
		int numOfPackages = 0, numOfClasses = 0;
		for (Entry<String, List<TypeDeclaration>> entry : Demo.project.getTypesAndPackages().entrySet()) {
			++numOfPackages;
			StringBuilder sb = new StringBuilder();
			sb.append("Package " + entry.getKey() + ": \n");
			int size = entry.getValue().size();
			numOfClasses += size;
			for (int i = 0; i < size; ++i) {
				TypeDeclaration tpd = entry.getValue().get(i);
				sb.append("\t");
				sb.append(tpd.getName());
				if (i != size-1)
					sb.append("\n");
			}
			CommonUtils.println(sb.toString());
		}
		CommonUtils.println(numOfPackages + " packages and " + numOfClasses + " classes are found in the project.");
		CommonUtils.println("");
	}
	
	
	static void removeDuplicatedTraces(List<List<AbstractCallGraphNode>> traces) {
		int size = traces.size();
		for (int i = 0; i < size-1; ++i) {
			for (int j = i+1; j < size; ++j) {
				if (traces.get(i).equals(traces.get(j))) {
					traces.remove(j);
					--size;
					--j;
				}
			}
		}
	}
}
