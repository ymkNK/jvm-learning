import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by ymkNK on 2021-04-26.
 */
public class HowJvmReflect {

    public static String PREFIX = "testcase";
    public static String EXIT = "exit";

    public static void testcase() {
        System.out.println("Can't find the function.This is a default test case");
    }

    /**
     * Created by ymk.
     * 1. 举例来说，我们可以通过 Class 对象枚举该类中的所有方法，我们还可以通过 Method.setAccessible（位于 java.lang.reflect 包，该方法继承自 AccessibleObject）绕过 Java 语言的访问权限，在私有方法所在类之外的地方调用该方法。
     **/
    public static void testcase1() throws IllegalAccessException, InvocationTargetException {
        System.out.println("===case 1===");
        CustomTest customTest = new CustomTest();
//        test.testPrivate();
//        System.out.println(test.privateValue);

        Class<CustomTest> testClass = CustomTest.class;

        for (Field declaredField : testClass.getDeclaredFields()) {
            declaredField.setAccessible(true);
            System.out.println("Test:" + declaredField.getName() + "=" + declaredField.get(customTest));
        }
        for (Method declaredMethod : testClass.getDeclaredMethods()) {
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(customTest);
        }

    }

    /**
     * Created by ymk.
     * 反射耗时的例子
     **/
    public static void testcase2() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        System.out.println("===case 2===");
        CustomTest customTest = new CustomTest();
        int invokeTimes = 100000;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < invokeTimes; i++) {
            customTest.testPublic();
        }
        long publicConsumeTime = System.currentTimeMillis() - startTime;

        Method privateMethod = CustomTest.class.getDeclaredMethod("testPrivate");
        privateMethod.setAccessible(true);
        startTime = System.currentTimeMillis();
        for (int i = 0; i < invokeTimes; i++) {
            customTest.testPublic();
            privateMethod.invoke(customTest);
        }
        long privateConsumeTime = System.currentTimeMillis() - startTime;

        System.out.println("直接调用耗时:" + publicConsumeTime + "ms");
        System.out.println("反射调用耗时:" + privateConsumeTime + "ms");
    }

    /**
     * Created by ymk.
     * 为了方便理解，我们可以打印一下反射调用到目标方法时的栈轨迹。
     * 在上面的 v0 版本代码中，我们获取了一个指向 Test.target 方法的 Method 对象，并且用它来进行反射调用。在 Test.target 中，我会打印出栈轨迹。
     * 可以看到，
     * 反射调用先是调用了 Method.invoke，
     * 然后进入委派实现（DelegatingMethodAccessorImpl），
     * 再然后进入本地实现（NativeMethodAccessorImpl），最后到达目标方法。
     **/
    public static void testcase3() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class klass = Class.forName("Test");
        Method method = klass.getMethod("target", int.class);
        method.invoke(null, 0);
    }

    public static void showEmptyLine() {
        System.out.println("\n");
    }

    public static void showMenu() {
        System.out.println("===Please select a function to test===\n" +
                "Type the number of test.\n" +
                "Type 'exit' to exit.");
    }

    public static void showExit() {
        System.out.println("===Exit bye bye===");
    }

    public static Map<String, Method> getAllFunction(Class<?> aClass) {
        Method[] declaredMethods = aClass.getDeclaredMethods();
        HashMap<String, Method> functionMap = new HashMap<>();
        for (Method method : declaredMethods) {
            functionMap.put(method.getName(), method);
        }
        return functionMap;
    }

    public static void main(String[] args) throws Exception {
        showMenu();
        Scanner scan = new Scanner(System.in);
        Map<String, Method> allFunction = getAllFunction(HowJvmReflect.class);
        while (scan.hasNext()) {
            String next = scan.next();
            next = next == null ? "" : next;
            if (EXIT.equals(next.toLowerCase())) {
                break;
            }
            String key = PREFIX + next;
            allFunction.getOrDefault(key, allFunction.get(PREFIX)).invoke(null);
            showEmptyLine();
            showMenu();
        }
        showExit();
        scan.close();
    }
}
