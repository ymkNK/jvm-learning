import java.lang.reflect.Method;

/**
 * Created by Yangmingkai on 2021/4/27.
 */
public class Test {
    public static void target1(int i) {
    }

    public static void target2(int i) {
    }

    public static void targetEmpty(int i) { // 空方法
    }

    public static void target(int i) {
        new Exception("#" + i).printStackTrace();
    }

    public static void main(String[] args) throws Exception {
        Class klass = Class.forName("Test");
        Method method = klass.getMethod("target", int.class);
        for (int i = 0; i < 20; i++) {
            method.invoke(null, i);
        }
    }

    public static void polluteProfile() throws Exception {
        Method method1 = Test.class.getMethod("target1", int.class);
        Method method2 = Test.class.getMethod("target2", int.class);
        for (int i = 0; i < 2000; i++) {
            method1.invoke(null, 0);
            method2.invoke(null, 0);
        }
    }
}


