class exp2_2
{
	public static void main(String args[])
	{
		System.out.println("Continue:");
		int i=0;
		while(i!=5)
		{
			i++;
			System.out.println(i);
			if(i==2)
			{
				continue;
			}
		}
		System.out.println("\nBreak:");
		int j=0;
		while(j!=5)
		{
			j++;
			System.out.println(j);
			if(j==2)
			
			{

				break;
			}
		}
	}
}