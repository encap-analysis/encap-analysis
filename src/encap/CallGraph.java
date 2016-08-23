package encap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.sun.javafx.util.Utils;

import edu.nju.cs.model.Project;
import edu.nju.cs.ui.Demo;

public class CallGraph {

	protected List<SimpleCallGraphNode> nodes = CommonUtils.newArrayList();
	
	private boolean isClassCGsInitialized = false;
	private boolean isTypeAndPackageInitialized = false;
	
	private boolean hasCycle = false;
	private List<List<AbstractCallGraphNode>> cycles = new ArrayList<>();
	
	private List<List<AbstractCallGraphNode>> sccs = new ArrayList<>();
	private List<List<AbstractCallGraphNode>> multiSccs = new ArrayList<>();

	public static CallGraph getCallGraph4Project(Project project) {
		CallGraph callgraph = new CallGraph();
		callgraph.build4Project(project);
		
		CommonUtils.println("\n====== The call graph of project " + project.getProjectAbsPath() + " is built successfully. ======\n");
		return callgraph;
	}
	
//	public static CallGraph getCallGraph4Class(TypeDeclaration classTypeDec) {
//		CallGraph callgraph = new CallGraph();
//		callgraph.build4Class(classTypeDec);
//		return callgraph;
//	}

	public CallGraph() {
		// do nothing
	}
	
	public CallGraph(CallGraph other) {
		for (SimpleCallGraphNode cgn : other.nodes) {
			this.nodes.add(new SimpleCallGraphNode(cgn));
		}
		
		for (SimpleCallGraphNode cgn : other.nodes) {
			SimpleCallGraphNode newCgn = findNodeByMtd(cgn.mtd);
			
			Set<AbstractCallGraphNode> newCallers = CommonUtils.newHashSet();
			for (AbstractCallGraphNode caller : cgn.callers) {
				SimpleCallGraphNode newCaller = findNodeByMtd(((SimpleCallGraphNode)caller).getMtd());
				newCallers.add(newCaller);
			}
			newCgn.setCallers(newCallers);
			
			Set<AbstractCallGraphNode> newCallees = CommonUtils.newHashSet();
			for (AbstractCallGraphNode callee : cgn.callees) {
				SimpleCallGraphNode newCallee = findNodeByMtd(((SimpleCallGraphNode)callee).mtd);
				newCallees.add(newCallee);
			}
			newCgn.setCallees(newCallees);
		}
		
		this.isClassCGsInitialized = other.isClassCGsInitialized;
		this.isTypeAndPackageInitialized = other.isTypeAndPackageInitialized;
		this.hasCycle = other.hasCycle;
		
		for (List<AbstractCallGraphNode> cycle : other.cycles) {
			List<AbstractCallGraphNode> newCycle = CommonUtils.newArrayList();
			for (AbstractCallGraphNode cgn : cycle) {
				newCycle.add(findNodeByMtd(((SimpleCallGraphNode)cgn).getMtd()));
			}
			this.cycles.add(newCycle);
		}
	}
	
	public void build4Project(Project project) {
		nodes.clear();
		
		Set<MethodDeclaration> mtds = project.getMtds();
		Set<MethodInvocation> mtis = project.getMtis();
		for (MethodDeclaration mtd : mtds) {
			if (CommonUtils.getDeclaringTpd(mtd) != null) {
				SimpleCallGraphNode cgn = new SimpleCallGraphNode(mtd);
				nodes.add(cgn);
			}
		}
		for (MethodInvocation mti : mtis) {
			MethodDeclaration callingMtd = CommonUtils.getCallingMtd(mti);
			if (callingMtd != null) {
				SimpleCallGraphNode caller = findNodeByMtd(callingMtd);
				
				MethodDeclaration calledMtd = CommonUtils.findDeclarationFor(mti);
				SimpleCallGraphNode callee = findNodeByMtd(calledMtd);
				if (caller != null && callee != null) {
					if (CommonUtils.getDeclaringTpd(callingMtd) != null && CommonUtils.getDeclaringTpd(calledMtd) != null) {
						caller.callees.add(callee);
						callee.callers.add(caller);
					}
				} else {
					//CommonUtils.debug();
				}
			}
			else {
				TypeDeclaration tpd = CommonUtils.getCallingTpd(mti);
				if (tpd == null)
					CommonUtils.println("Error: calling type declaration not found!");
			}
		}
	}
	
	public void build4Class(TypeDeclaration classTypeDec) {
		nodes.clear();
		
		if (classTypeDec.isInterface())
			return;
		
		Set<MethodDeclaration> mtds = Demo.project.getMtds();
		Set<MethodInvocation> mtis = Demo.project.getMtis();
		
		for (MethodDeclaration mtd : mtds) {
			TypeDeclaration declaringTpd = CommonUtils.getDeclaringTpd(mtd);
			if (declaringTpd == classTypeDec) {
				SimpleCallGraphNode cgn = new SimpleCallGraphNode(mtd);
				nodes.add(cgn);
			}
		}
		for (MethodInvocation mti : mtis) {
			MethodDeclaration callingMtd = CommonUtils.getCallingMtd(mti);
			if (callingMtd != null) {
				SimpleCallGraphNode caller = findNodeByMtd(callingMtd);
				
				MethodDeclaration calledMtd = CommonUtils.findDeclarationFor(mti);
				SimpleCallGraphNode callee = findNodeByMtd(calledMtd);
				if (caller != null && callee != null) {
					caller.callees.add(callee);
					callee.callers.add(caller);
				}
			}
			else {
				TypeDeclaration tpd = CommonUtils.getCallingTpd(mti);
				if (tpd == null)
					CommonUtils.println("Error: calling type declaration not found!");
			}
		}
		
	}
	
	public void analyze() {
		initClassCGs();
		initTypeAndPackageInfo();
//		detectCycles();
//		reportCycles();
		
		detectSccs();
		
		CommonUtils.println("\n====== Doing analysis on the call graph... ======\n");
		//CommonUtils.println(toString());
		
		CallDag calldag = new CallDag(this);
		//CommonUtils.println(calldag.toString());
		calldag.analyze();
	}

	public void initClassCGs() {
		if (isClassCGsInitialized)
			return;
		
		InheritTree it = InheritTree.getInheritTree4Project(Demo.project);
		it.initClassCGs();
		
		isClassCGsInitialized = true;
	}

	void initTypeAndPackageInfo() {
		if (isTypeAndPackageInitialized)
			return;
		
		for(String cuName : Demo.projectExt.projectUnits.keySet()) {
			CompilationUnit cu = Demo.projectExt.projectUnits.get(cuName);
			PackageDeclaration pd = cu.getPackage();
			cu.accept(new PackageInfoVisitor(pd));
		}
		isTypeAndPackageInitialized = true;
		GraphUtils.printTypesInPackges();
	}

	public void detectCycles() {
		CommonUtils.println("====== Detecting cycles... ======");
		for (SimpleCallGraphNode node : nodes) {
			Set<AbstractCallGraphNode> visited = new HashSet<>();
			List<AbstractCallGraphNode> trace = new ArrayList<>();
			findCycles(node, visited, trace);
		}
	}

	public void findCycles(AbstractCallGraphNode node, Set<AbstractCallGraphNode> visited, List<AbstractCallGraphNode> trace) {
		if (visited.contains(node)) {
			int index = trace.indexOf(node);
			if (index != -1) {
				hasCycle = true;
				List<AbstractCallGraphNode> cycle = new ArrayList<>();
				while (index < trace.size()) {
					cycle.add(trace.get(index));
					++index;
				}
				addAfterRotationTest(cycle);
				return;
			}
			return;
		}
		visited.add(node);
		trace.add(node);
		
		for (AbstractCallGraphNode callee : node.callees) {
			findCycles(callee, visited, trace);
		}
		
		trace.remove(trace.size()-1);
	}
	
	void addAfterRotationTest(List<AbstractCallGraphNode> cycle) {
		if (!cycles.isEmpty()) {
			for (int i = 1; i <= cycle.size(); ++i) {
				Collections.rotate(cycle, 1);
				if (cycles.contains(cycle)) {
					return;
				}
			}
		}
		cycles.add(cycle);
	}
	
	public boolean hasCycle() {
		return hasCycle;
	}
	
	public List<List<AbstractCallGraphNode>> getCycles() {
		return cycles;
	}

	public void reportCycles() {
		CommonUtils.println(nodes.size() + " method(s) and " + cycles.size() + " cycle(s) are found in the call graph.\n");
		
		if (hasCycle())
			printCycles();
	}

	public void printCycles() {
		CommonUtils.println("=== Printing cycles... ===");
		
		for (List<AbstractCallGraphNode> cycle : cycles) {
			CommonUtils.println("Cycle:");
			for (AbstractCallGraphNode node : cycle) {
				CommonUtils.print("\t" + node.toSimpleStr() + "\n");
			}
		}
	}

	public boolean isInCycle(AbstractCallGraphNode cgn) {
		return isInCycle(((SimpleCallGraphNode)cgn).getMtd());
	}
	
	public boolean isInCycle(MethodDeclaration mtd) {
		for (List<AbstractCallGraphNode> cycle : cycles) {
			for (AbstractCallGraphNode node : cycle) {
				if (((SimpleCallGraphNode)node).correspondTo(mtd))
					return true;
			}
		}
		return false;
	}
	
	public List<AbstractCallGraphNode> getCycle(AbstractCallGraphNode cgn) {
		return getCycle(((SimpleCallGraphNode)cgn).getMtd());
	}
	
	public List<AbstractCallGraphNode> getCycle(MethodDeclaration mtd) {
		for (List<AbstractCallGraphNode> cycle : cycles) {
			for (AbstractCallGraphNode node : cycle) {
				if (((SimpleCallGraphNode)node).correspondTo(mtd))
					return cycle;
			}
		}
		return CommonUtils.newArrayList();
	}
	
	transient Integer[] dfn = null;
	transient Integer[] low = null;
	transient Integer[] belong = null;
	transient Boolean[] instack = null;
	
	transient Stack<Integer> stack = new Stack<>();
	transient Integer index = 0, group = 0;

	void detectSccs() {
		dfn = new Integer[nodes.size()];
		low = new Integer[nodes.size()];
		belong = new Integer[nodes.size()];
		instack = new Boolean[nodes.size()];
		Arrays.fill(dfn, -1);
		Arrays.fill(low, -1);
		Arrays.fill(belong, -1);
		Arrays.fill(instack, false);
		
		for (int i = 0; i < nodes.size(); ++i)
			if (dfn[i] == -1)
				tarjan(i);
		
		for (int i = 0; i < group; ++i) {
			List<AbstractCallGraphNode> scc = CommonUtils.newArrayList();
			for (int j = 0; j < nodes.size(); ++j)
				if (belong[j] == i)
					scc.add(nodes.get(j));
			
			sccs.add(scc);
		}
		
		int size = 0;
		for (List<AbstractCallGraphNode> scc : sccs) {
			size += scc.size();
			if (scc.size() != 1)
				multiSccs.add(scc);
		}
		
		CommonUtils.println(nodes.size() + " nodes and " + sccs.size() + " sccs found in the call graph.");
		
		if (size != nodes.size())
			CommonUtils.debug("error ocurred in tarjan.");
		
	}
	
	void tarjan(int i) {
		dfn[i] = low[i] = index++;
		stack.push(i);
		instack[i] = true;
		
		int j;
		for (AbstractCallGraphNode callee : nodes.get(i).getCallees()) {
			j = getIndexOf(callee);
			if (dfn[j] == -1) {
				tarjan(j);
				if (low[j] < low[i])
					low[i] = low[j];
			} else if (instack[j] && dfn[j] < low[i]) {
				low[i] = dfn[j];
			}
		}
		
		if (dfn[i] == low[i]) {
			do {
				j = stack.pop();
				instack[j] = false;
				belong[j] = group;
			} while (j != i);
			group++;
		}
	}
	
	int getIndexOf(AbstractCallGraphNode acgn) {
		for (int i = 0; i < nodes.size(); ++i) {
			AbstractCallGraphNode node  = nodes.get(i);
			if (acgn.equals(node))
				return i;
		}
		return -1;
	}
	
	List<List<AbstractCallGraphNode>> getSccs() {
		return sccs;
	}
	
	boolean isInScc(AbstractCallGraphNode cgn) {
		return isInScc(((SimpleCallGraphNode)cgn).getMtd());
	}

	boolean isInScc(MethodDeclaration mtd) {
		for (List<AbstractCallGraphNode> scc : sccs) {
			if (scc.size() == 1)
				continue;
			for (AbstractCallGraphNode node : scc) {
				if (((SimpleCallGraphNode)node).correspondTo(mtd))
					return true;
			}
		}
		return false;
	}

	public List<AbstractCallGraphNode> getScc(AbstractCallGraphNode cgn) {
		return getScc(((SimpleCallGraphNode)cgn).getMtd());
	}

	public List<AbstractCallGraphNode> getScc(MethodDeclaration mtd) {
		for (List<AbstractCallGraphNode> scc : sccs) {
			for (AbstractCallGraphNode node : scc) {
				if (((SimpleCallGraphNode)node).correspondTo(mtd))
					return scc;
			}
		}
		return CommonUtils.newArrayList();
	}

	public void evaluateAll() {
		for (Entry<TypeDeclaration, ClassCG> entry : ClassCG.tpd2ccg.entrySet()) {
			TypeDeclaration tpd = entry.getKey();
			ClassCG ccg = entry.getValue();
			ClassCG.setCurrTpd(tpd);
			ClassCG.setCurrObjNode(ClassCG.tpd2obj.get(tpd));
			ccg.evaluate();
		}
		
		CommonUtils.println("EncapClassCount: " + ClassCG.encapClassCount);
		CommonUtils.println("NotEncapClassCount: " + ClassCG.notEncapClassCount);
		CommonUtils.println("ArgClassCount: " + ClassCG.argClassCount);
		CommonUtils.println("RetClassCount: " + ClassCG.retClassCount);
	}
	
	public List<List<AbstractCallGraphNode>> getCallTraces(MethodDeclaration mtd) {
		SimpleCallGraphNode cgn = findNodeByMtd(mtd);
		List<List<AbstractCallGraphNode>> traces = getCallTraces(cgn, 10);
		GraphUtils.removeDuplicatedTraces(traces);
		return traces;
	}
	
	public List<List<AbstractCallGraphNode>> getCallTraces(AbstractCallGraphNode cgn, int maxDepth) {
		List<List<AbstractCallGraphNode>> callTraces = CommonUtils.newArrayList();
		if (maxDepth > 0 && cgn != null) {
			if (cgn.callers.isEmpty()) {
				List<AbstractCallGraphNode> callTrace = CommonUtils.newArrayList();
				callTrace.add(cgn);
				callTraces.add(callTrace);
			} else {
				if (isInScc(cgn)) {
					callTraces = getSccTraces(cgn, maxDepth);
				} else {
					for (AbstractCallGraphNode caller : cgn.callers) {
						if (isInScc(caller)) {
							callTraces = getSccTraces(caller, maxDepth-1);
						} else {
							List<List<AbstractCallGraphNode>> subTraces = getCallTraces(caller, maxDepth-1);
							for (List<AbstractCallGraphNode> subTrace : subTraces) {
								List<AbstractCallGraphNode> backTrace = CommonUtils.newArrayList();
								backTrace.addAll(subTrace);
								callTraces.add(backTrace);
							}
						}
					}
					for (List<AbstractCallGraphNode> backTrace : callTraces) {
						backTrace.add(cgn);
					}
				}
			}
		}
		return callTraces;
	}

	// pre: isInCycle(cgn)
	List<List<AbstractCallGraphNode>> getSccTraces(AbstractCallGraphNode cgn, int maxDepth) {
		List<List<AbstractCallGraphNode>> backTraces = CommonUtils.newArrayList();
		List<AbstractCallGraphNode> scc = getScc(cgn);
		List<AbstractCallGraphNode> callers = GraphUtils.getUniqueCallersOfScc(scc);
		for (AbstractCallGraphNode callerOfScc : callers) {
			List<List<AbstractCallGraphNode>> subTraces = getCallTraces((SimpleCallGraphNode)callerOfScc, maxDepth-1);
			for (List<AbstractCallGraphNode> subTrace : subTraces) {
				List<AbstractCallGraphNode> backTrace = CommonUtils.newArrayList();
				backTrace.addAll(subTrace);
				backTrace.add(SimpleCallGraphNode.SCC_BEGIN);
				backTrace.addAll(GraphUtils.getProperCallTraceOrder(callerOfScc, scc));
				backTrace.add(SimpleCallGraphNode.SCC_END);
				backTraces.add(backTrace);
			}
		}
		return backTraces;
	}

	public List<SimpleCallGraphNode> getNodes() {
		return nodes;
	}

	SimpleCallGraphNode findNodeByMtd(MethodDeclaration mtd) {
		for (SimpleCallGraphNode cgn : nodes) {
			if (cgn.correspondTo(mtd))
				return cgn;
		}
		return null;
	}
	
	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CallGraph: \n");
		for (SimpleCallGraphNode scgn : nodes) {
			sb.append(scgn.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
}