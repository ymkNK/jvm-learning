import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by ymkNK on 2021-04-26.
 */
public class TestCase {

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
        int invokeTimes = 1000000;
        Method privateMethod = CustomTest.class.getDeclaredMethod("testPublic");
        privateMethod.setAccessible(true);
        for (int i = 0; i < invokeTimes; i++) {
            privateMethod.invoke(customTest);
        }
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < invokeTimes; i++) {
            privateMethod.invoke(customTest);
        }
        long publicConsumeTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        for (int i = 0; i < invokeTimes; i++) {
            customTest.testPublic();
        }
        long privateConsumeTime = System.currentTimeMillis() - startTime;

        System.out.println("反射调用耗时:" + publicConsumeTime + "ms");
        System.out.println("直接调用耗时:" + privateConsumeTime + "ms");
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

    /**
     * Created by ymk.
     * 考虑到许多反射调用仅会执行一次，Java 虚拟机设置了一个阈值 15（可以通过 -Dsun.reflect.inflationThreshold= 来调整），
     * 当某个反射调用的调用次数在 15 之下时，采用本地实现；
     * 当达到 15 时，便开始动态生成字节码，并将委派实现的委派对象切换至动态实现，这个过程我们称之为 Inflation。
     * <p>
     * -verbose:class
     * <p>
     * 可以看到，在第 15 次（从 0 开始数）反射调用时，我们便触发了动态实现的生成。
     * 这时候，Java 虚拟机额外加载了不少类。其中，最重要的当属 GeneratedMethodAccessor1。
     * 并且，从第 16 次反射调用开始，我们便切换至这个刚刚生成的动态实现（第 40 行）
     * 。反射调用的 Inflation 机制是可以通过参数（-Dsun.reflect.noInflation=true）来关闭的。
     * 这样一来，在反射调用一开始便会直接生成动态实现，而不会使用委派实现或者本地实现。
     * <p>
     * -Dsun.reflect.noInflation=true
     **/
    public static void testcase4() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class klass = Class.forName("Test");
        Method method = klass.getMethod("target", int.class);
        for (int i = 0; i < 20; i++) {
            method.invoke(null, i);
        }
    }

    /**
     * Created by ymk.
     * 直接调用作为基准
     * v1
     **/
    public static void testcase5() {
        long current = System.currentTimeMillis();
        for (int i = 1; i <= 2_000_000_000; i++) {
            if (i % 100_000_000 == 0) {
                long temp = System.currentTimeMillis();
                System.out.println(temp - current);
                current = temp;
            }
            Test.targetEmpty(128);
        }
    }

    /**
     * Created by ymk.
     * 为了比较直接调用和反射调用的性能差距，我将前面的例子改为下面的 v2 版本。它会将反射调用循环二十亿次。
     * 此外，它还将记录下每跑一亿次的时间。我将取最后五个记录的平均值，作为预热后的峰值性能。（注：这种性能评估方式并不严谨，我会在专栏的第三部分介绍如何用 JMH 来测性能。）
     * <p>
     * 这两个操作除了带来性能开销外，还可能占用堆内存，使得 GC 更加频繁。（如果你感兴趣的话，可以用虚拟机参数 -XX:+PrintGC 试试。）那么，如何消除这部分开销呢？
     * 关于第二个自动装箱，Java 缓存了[-128, 127]中所有整数所对应的 Integer 对象。
     * 当需要自动装箱的整数在这个范围之内时，便返回缓存的 Integer，否则需要新建一个 Integer 对象。
     * 因此，我们可以将这个缓存的范围扩大至覆盖 128（对应参数-Djava.lang.Integer.IntegerCache.high=128），便可以避免需要新建 Integer 对象的场景。
     * <p>
     * -Djava.lang.Integer.IntegerCache.high=128
     * v2
     **/
    public static void testcase6() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class klass = Class.forName("Test");
        Method method = klass.getMethod("targetEmpty", int.class);
        long current = System.currentTimeMillis();
        for (int i = 1; i <= 2_000_000_000; i++) {
            if (i % 100_000_000 == 0) {
                long temp = System.currentTimeMillis();
                System.out.println(temp - current);
                current = temp;
            }
            method.invoke(null, 128);
        }
    }

    /**
     * Created by ymk.
     * 现在我们再回来看看第一个因变长参数而自动生成的 Object 数组。既然每个反射调用对应的参数个数是固定的，那么我们可以选择在循环外新建一个 Object 数组，设置好参数，并直接交给反射调用。
     * v3
     **/
    public static void testcase7() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class klass = Class.forName("Test");
        Method method = klass.getMethod("targetEmpty", int.class);
        Object[] arg = new Object[1];
        // 在循环外构造参数数组
        arg[0] = 128;
        long current = System.currentTimeMillis();
        for (int i = 1; i <= 2_000_000_000; i++) {
            if (i % 100_000_000 == 0) {
                long temp = System.currentTimeMillis();
                System.out.println(temp - current);
                current = temp;
            }
            method.invoke(null, arg);
        }
    }


    /**
     * Created by ymk.
     * // 在运行指令中添加如下两个虚拟机参数：
     * // -Djava.lang.Integer.IntegerCache.high=128
     * // -Dsun.reflect.noInflation=true
     * 同时关闭权限检查 v4
     **/
    public static void testcase8() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class klass = Class.forName("Test");
        Method method = klass.getMethod("targetEmpty", int.class);
        method.setAccessible(true);
        // 关闭权限检查
        long current = System.currentTimeMillis();
        for (int i = 1; i <= 2_000_000_000; i++) {
            if (i % 100_000_000 == 0) {
                long temp = System.currentTimeMillis();
                System.out.println(temp - current);
                current = temp;
            }
            method.invoke(null, 128);
        }
    }

    /**
     * Created by ymk.
     * 在上面的 v5 版本中，我在测试循环之前调用了 polluteProfile 的方法。该方法将反射调用另外两个方法，并且循环上 2000 遍。而测试循环则保持不变。测得的结果约为基准的 6.7 倍。
     * 也就是说，只要误扰了 Method.invoke 方法的类型 profile，性能开销便会从 1.3 倍上升至 6.7 倍。
     * 之所以这么慢，除了没有内联之外，另外一个原因是逃逸分析不再起效。这时候，我们便可以采用刚才 v3 版本中的解决方案，在循环外构造参数数组，并直接传递给反射调用。这样子测得的结果约为基准的 5.2 倍。
     * v5
     * -XX:TypeProfileWidth
     **/
    public static void testcase9() throws Exception {
        Class klass = Class.forName("Test");
        Method method = klass.getMethod("targetEmpty", int.class);
        method.setAccessible(true);
        Test.polluteProfile();
        // 关闭权限检查
        long current = System.currentTimeMillis();
        for (int i = 1; i <= 2_000_000_000; i++) {
            if (i % 100_000_000 == 0) {
                long temp = System.currentTimeMillis();
                System.out.println(temp - current);
                current = temp;
            }
            method.invoke(null, 128);
        }
    }

    /**
     * Created by ymk.
     * 今天的实践环节，你可以将最后一段代码中 polluteProfile 方法的两个 Method 对象，都改成获取名字为“target”的方法。
     * 请问这两个获得的 Method 对象是同一个吗（==）？他们 equal 吗（.equals(…)）？对我们的运行结果有什么影响？
     **/
    public static void testcase10() throws NoSuchMethodException {

        Method method1 = Test.class.getDeclaredMethod("target1", int.class);
        Method method2 = Test.class.getDeclaredMethod("target1", int.class);
        method2.setAccessible(true);
        System.out.println("'method1 == method2' is:" + (method1 == method2));
        System.out.println("'method3 equals method4' is:" + (method1.equals(method2)));
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
        Map<String, Method> allFunction = getAllFunction(TestCase.class);
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
