// testando this (OK)
class m336
{
   public static void main(String[] args)
   {
      System.out.println(new b().j());
   }
}

class a
{
   int k;
   public int i(){ 
	return 0; }
}

class b extends a
{
	int x;
	public int j() { 
		k = 5;
		System.out.println(k);
		return 1; 
		}
}