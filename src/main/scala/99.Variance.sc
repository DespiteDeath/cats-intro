class Foo[+A] // A covariant class
class Bar[-A] // A contravariant class
class Baz[A]  // An invariant class

class X[-A]
class Y[+A]
class Z[A]

//contravariant
val x: X[String] = new X[Object] //Specialize -, should not be exposed
//covariant
val y: Y[Object] = new Y[String] //Generalize +, can be exposed
//invariant
val z: Z[String] = new Z[String]

class Emp
class Manager extends Emp
class CEO extends Manager
class Award[+T](val recipient: T) //co, +, extends, Generalize, up, get, source
class Problem[-T](recipient: T) //contra, -, super,  Specialize, down, put, sink
class Action1[+T <: Manager](val recipient: T) //Specialize < extends down
class Action2[-T >: Manager](recipient: T) //Generalize > super up

// Function[-A, +B]
val f1: Function[Manager, Manager] = _ => new CEO
val f2: Function[CEO, Emp] = f1

val ea: Award[Emp] = new Award[Manager](new Manager)
//val ca: Award[CEO] = new Award[Manager](new Manager)

val ep: Problem[Manager] = new Problem[Emp](new Manager)

val action1: Action1[Manager] = new Action1[CEO](new CEO)
val action2: Action2[Manager] = new Action2[Emp](new Emp)
