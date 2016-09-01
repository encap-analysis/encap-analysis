# Encap-analysis

Encapsulation analysis is an expression-based dataflow analysis that statically computes the runtime memory layouts of object-oriented programs. Its analysis results identify the classes that have potential encapsulation problem, the class members that are shared and the methods that expose the members.

Our approach is designed and implemented for Java, which is a relatively pure OO style language and there is only reference semantics for objects. It can be ported to other object-oriented languages by considering their own semantics w.r.t. encapsulation. 

A more detailed description can be found [here](https://encap-analysis.github.io/encap-analysis/).
