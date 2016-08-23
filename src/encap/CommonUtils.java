package encap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

class Pair<T1, T2> {
	
	public final T1 value1;
	
	public final T2 value2;
	
	public Pair(T1 v1, T2 v2){
		this.value1 = v1;
		this.value2 = v2;
	}
	
	public static <T1, T2> Pair<T1, T2> of(T1 v1, T2 v2) {
		return new Pair<T1, T2>(v1, v2);
	}
	
}

class Tuple<T1, T2, T3> {
	
	public final T1 value1;
	
	public final T2 value2;
	
	public final T3 value3;
	
	public Tuple(T1 v1, T2 v2, T3 v3){
		this.value1 = v1;
		this.value2 = v2;
		this.value3 = v3;
	}
	
	public static <T1, T2, T3> Tuple<T1, T2, T3> of(T1 v1, T2 v2, T3 v3) {
		return new Tuple<T1, T2, T3>(v1, v2, v3);
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Tuple))
			return false;
		
		Tuple<?, ?, ?> tuple = (Tuple<?, ?, ?>)obj;
		
		return this.value1.equals(tuple.value1) 
			&& this.value2.equals(tuple.value2) 
			&& this.value3.equals(tuple.value3);
	}
	
}

public class CommonUtils {
	
	private static boolean printDebug = false;
	
	public static void openWriter() {
		if (writer == null)
			try {
				writer = new BufferedWriter(new FileWriter("D:\\EncapLog.txt"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public static void closeWriter() {
		if (writer != null)
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	static BufferedWriter writer = null;
	
	public static <T> ArrayList<T> newArrayList() {
		return new ArrayList<T>();
	}	
	
	public static <T1, T2> HashMap<T1, T2> newHashMap() {
		return new HashMap<T1, T2>();
	}	
	
	public static <T> HashSet<T> newHashSet() {
		return new HashSet<T>();
	}
	
	public static <T> TreeSet<T> newTreeSet() {
		return new TreeSet<T>();
	}	
	
	public static <T> ArrayDeque<T> newArrayDeque() {
		return new ArrayDeque<T>();
	}
	
	public static void print(String str) {
		//System.out.print(str);
		log(str);
	}
	
	public static void println(String str) {
		//System.out.println(str);
		logln(str);
	}

	public static void printlnError(String str) {
		System.err.println(str);	
	}
	
	public static void log(String str) {
		try {			
			writer.write(str);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void logln(String str) {
		try {		
			writer.write(str);
			writer.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void debug() {
		if (printDebug)
			System.out.println("debug");
	}
	
	public static void debug(String str) {
		if (printDebug)
			System.out.println("debug: " + str);
	}

	public static Boolean isField(SimpleName simpleName) {
		IVariableBinding ivb = getVarBinding(simpleName);
		if (ivb != null)
			return ivb.isField();
		return null;
	}

	public static Boolean isStatic(SimpleName simpleName) {
		IVariableBinding ivb = getVarBinding(simpleName);
		if (ivb != null)
			return Modifier.isStatic(ivb.getModifiers());
		return null;
	}

	public static Boolean isParameter(SimpleName simpleName) {
		IVariableBinding ivb = getVarBinding(simpleName);
		if (ivb != null)
			return ivb.isParameter();
		return null;
	}

	public static Boolean isPrivate(SimpleName simpleName) {
		IVariableBinding ivb = getVarBinding(simpleName);
		if (ivb != null)
			return Modifier.isPrivate(ivb.getModifiers());
		return null;
	}

	public static Boolean isProtected(SimpleName simpleName) {
		IVariableBinding ivb = getVarBinding(simpleName);
		if (ivb != null)
			return Modifier.isProtected(ivb.getModifiers());
		return null;
	}

	public static Boolean isPublic(SimpleName simpleName) {
		IVariableBinding ivb = getVarBinding(simpleName);
		if (ivb != null)
			return Modifier.isPublic(ivb.getModifiers());
		return null;
	}

	public static Boolean isPrimitive(SimpleName simpleName) {
		ITypeBinding itb = simpleName.resolveTypeBinding();
		if (itb == null)
			return null;
		return simpleName.resolveTypeBinding().isPrimitive();
	}

	public static IVariableBinding getVarBinding(SimpleName simpleName) {
		IBinding ib = simpleName.resolveBinding();
		int kind = ib.getKind();
		
		if (kind != IBinding.VARIABLE)
			return null;
	
		return (IVariableBinding)ib;
	}
	
	public static ITypeBinding getTypeBinding(SimpleName simpleName) {
		IBinding ib = simpleName.resolveBinding();
		int kind = ib.getKind();
		
		if (kind != IBinding.TYPE)
			return null;
	
		return (ITypeBinding)ib;
	}
	
	
	public static String getMethodSignature(MethodDeclaration mtd) {
		StringBuilder sb = new StringBuilder();
		List<SingleVariableDeclaration> params = mtd.parameters();
		sb.append(mtd.getName()).append("(");
		int size = params.size();
		for (int i = 0; i < size; ++i) {
			SingleVariableDeclaration param = params.get(i);
			sb.append(param.getType().toString() + " " + param.getName());
			if (i != size-1)
				sb.append(", ");
		}
		sb.append(")");
		return sb.toString();
	}
	
	public static TypeDeclaration getTyd(ClassInstanceCreation cic) {
		TypeDeclaration tpd = null;
		Type type = cic.getType();
		ITypeBinding itb = type.resolveBinding();
		if (itb == null)
			return null;
		String typeName = itb.getQualifiedName();
		if (itb.isClass()) {
			ASTNode node = Demo.project.getTypeDecByName(typeName);
			if (node instanceof TypeDeclaration) 
				tpd = (TypeDeclaration) node;
			
			return null;
		}
		return tpd;
	}
	
	public static TypeDeclaration getTyd(SingleVariableDeclaration svd) {
		String typeName = getTypeName(svd);
		TypeDeclaration tpd = (TypeDeclaration)Demo.project.getClassByName(typeName);
		return tpd;
	}
	
	public static String getTypeName(ASTNode astnode) {
		if (astnode instanceof ClassInstanceCreation) {
			Type type = ((ClassInstanceCreation)astnode).getType();
			ITypeBinding itb = type.resolveBinding();
			return itb.getQualifiedName();
		}
		else if (astnode instanceof SingleVariableDeclaration) {
			IVariableBinding ivb = ((SingleVariableDeclaration)astnode).resolveBinding();
			if (ivb == null)
				return null;
			return ivb.getType().getQualifiedName();
		}
		return null;
	}
	
	public static String getTpdName(TypeDeclaration tpd) {
		ITypeBinding itb = tpd.resolveBinding();
		if (itb != null) {
			return itb.getQualifiedName();
		}
		return null;
	}

	public static MethodDeclaration findDeclarationFor(MethodInvocation mi){
		IMethodBinding binding = (IMethodBinding) mi.getName().resolveBinding();
		String methodStr = MethodUtils.getMethodIdStr(binding);
		MethodDeclaration mtd = Demo.project.getMethodDecFromIdString(methodStr);
		
		return mtd;
	}
	
	public static MethodDeclaration getCallingMtd(MethodInvocation mti) {
		ASTNode astnode;
		for (astnode = mti.getParent(); astnode != null && !(astnode instanceof MethodDeclaration); astnode = astnode.getParent())
			;
		if (astnode != null && astnode instanceof MethodDeclaration)
			return (MethodDeclaration)astnode;
		return null;
	}
	
	public static TypeDeclaration getCallingTpd(MethodInvocation mti) {
		if (getCallingMtd(mti) != null)
			return null;
		
		ASTNode astnode;
		for (astnode = mti.getParent(); astnode != null && !(astnode instanceof Initializer) && !(astnode instanceof TypeDeclaration); astnode = astnode.getParent())
			;
		
		if (astnode != null && astnode instanceof TypeDeclaration) {
			return (TypeDeclaration)astnode;
		}
		if (astnode != null && astnode instanceof Initializer) {
			Initializer i = (Initializer)astnode;
//			if (Modifier.isStatic(i.getModifiers()))
//				Utils.println("Static initializer found");
//			else
//				Utils.println("Instance initializer found");
			
			// only consider TypeDeclaration，ommit EnumDeclaration and AnnotationTypeDeclaration
			if (i.getParent() instanceof TypeDeclaration)
				return (TypeDeclaration)i.getParent();
		}

		return null;
	}
	
	public static TypeDeclaration getDeclaringTpd(MethodDeclaration mtd) {
		ASTNode astnode;
		for (astnode = mtd.getParent(); astnode != null && !(astnode instanceof TypeDeclaration); astnode = astnode.getParent())
			;
		if (astnode != null && astnode instanceof TypeDeclaration)
			return (TypeDeclaration)astnode;
		return null;
	}
	
	public static AnonymousClassDeclaration getDeclaringAcd(MethodDeclaration mtd) {
		ASTNode astnode;
		for (astnode = mtd.getParent(); astnode != null && !(astnode instanceof AnonymousClassDeclaration); astnode = astnode.getParent())
			;
		if (astnode != null && astnode instanceof AnonymousClassDeclaration)
			return (AnonymousClassDeclaration)astnode;
		return null;
	}
	
	public static boolean isDeclaredInAcd(MethodDeclaration mtd) {
		if (mtd != null && mtd.getParent() != null && mtd.getParent() instanceof AnonymousClassDeclaration)
			return true;
		return false;
		
//		ITypeBinding itb = mtd.resolveBinding().getDeclaringClass();
//		return itb.isAnonymous();
	}
	
	public static void rotate(int[] arr, int order) {	
		if (arr == null || arr.length==0 || order < 0) {
			throw new IllegalArgumentException("Illegal argument!");
		}
	 
		if(order > arr.length){
			order = order %arr.length;
		}
	 
		//length of first part
		int a = arr.length - order; 
	 
		reverse(arr, 0, a-1);
		reverse(arr, a, arr.length-1);
		reverse(arr, 0, arr.length-1);
	 
	}
	 
	public static void reverse(int[] arr, int left, int right){
		if(arr == null || arr.length == 1) 
			return;
	 
		while(left < right){
			int temp = arr[left];
			arr[left] = arr[right];
			arr[right] = temp;
			left++;
			right--;
		}	
	}
}
