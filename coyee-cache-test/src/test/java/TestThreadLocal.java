import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author hxfein
 * @className: TestThreadLocal
 * @description:
 * @date 2022/6/14 11:50
 * @version：1.0
 */
public class TestThreadLocal {
    private static ThreadLocal<List<String>> threadLocal = new TransmittableThreadLocal<>();

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 1000; i++) {
            final int index = i;
            executorService.execute(() -> {
                List<String> list = threadLocal.get();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(String.valueOf(index));
                threadLocal.set(list);
                System.out.println(Thread.currentThread().getName() + "  " + list);
            });
        }
    }
}
