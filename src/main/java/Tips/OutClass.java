package Tips;

public class OutClass {

    public static String out = "out";
    static {
        System.out.println("OutClass代码块");
    }

    static class InnerClass{
        public static String in = "in";
        static {
            System.out.println("InnerClass代码块");
        }
    }

}


