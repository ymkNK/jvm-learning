import java.util.Random;

/**
 * Created by Yangmingkai on 2021/4/26.
 */
public class CustomTest {
    private void testPrivate() {
        System.out.println("this is a private method in Test.class");
    }
    public void testPublic(){
        Random random = new Random();
        random.nextInt();
    }

    private int privateValue = 1;

}
