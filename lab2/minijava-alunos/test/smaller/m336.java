// testando this (OK)
class m336
{
   public static void main(String[] args)
   {
      System.out.println(new C().cab(new B()));
   }
}

class A
{
   int k;
   public int i(){ 
	System.out.println(6);
	return 0; 
	}
}

class B extends A
{
	int x;
	public int j(int cap) { 
		k = 5;
		System.out.println(k);
		return 1; 
		}
}

class C {
	public int cab(A elem) {
		int aux;
		aux = elem.i();
		return 1;
	}
}