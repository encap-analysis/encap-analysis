package encap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MethodOverrideRelationBuilder {
	
	TypeDeclaration superTypeDec = null;
	TypeDeclaration subTypeDec = null;
	
	public MethodOverrideRelationBuilder(TypeDeclaration superTpd, TypeDeclaration subTpd) {
		superTypeDec = superTpd;
		subTypeDec = subTpd;
	}
	
	// Build the override relationship between superTypdDec and subTypeDec
	public void build() {
		MethodDeclaration[] subTypeMethods = subTypeDec.getMethods();
		MethodDeclaration[] superTypeMethods = superTypeDec.getMethods();
		for (MethodDeclaration subTypeMtd : subTypeMethods) {
			IMethodBinding imbSub = subTypeMtd.resolveBinding();
			boolean found = false;
			for (MethodDeclaration superTypeMtd : superTypeMethods) {
				IMethodBinding imbSuper = superTypeMtd.resolveBinding();
				if (imbSub.overrides(imbSuper)) {
					Demo.project.connectOverridingAndOverriden(subTypeMtd, superTypeMtd);
					found = true;
				}
			}
			if (!found) {
				String superSuperClassName = Demo.project.getSuperClassName(superTypeDec.resolveBinding().getQualifiedName());
				// 优先考虑class，其次考虑interface
				ASTNode superSuperTypeDec = Demo.project.getClassByName(superSuperClassName);
				if (superSuperTypeDec == null)
					superSuperTypeDec = Demo.project.getInterfaceByName(superSuperClassName);
				
				if (superSuperTypeDec != null)
					new MethodOverrideRelationBuilder((TypeDeclaration) superSuperTypeDec, subTypeDec).build();
			}
		}
	}
	
}
