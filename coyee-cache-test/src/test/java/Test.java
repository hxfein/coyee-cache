import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hxfein
 * @className: Test
 * @description: test
 * @date 2022/6/2 18:46
 * @version：1.0
 */
public class Test {
    static AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) {
        ExecutorService executorService=Executors.newFixedThreadPool(30);
        for(int i=0;i<100000;i++) {
            executorService.execute(new GetThread());
            if(i%200==0) {
                executorService.execute(new UpdateThread());
            }
            sleep(100);
        }
    }

    static class GetThread implements Runnable {
        public void run() {
            String urlStr="http://localhost:7777/getUserAndRoleList?keyword="+ RandomStringUtils.random(5,"abcdefghijjklsas");
            String url=String.format(urlStr);
            long start=System.currentTimeMillis();
            String result=catchUrl(url);
            long end=System.currentTimeMillis();
            System.out.println("****获取数据结果:"+result+"，用时:"+(end-start)+"   "+urlStr);
        }
    }

    static class UpdateThread implements Runnable {
        public void run() {
            String url=String.format("http://localhost:7777/updateRole?keyword="+Math.random());
            long start=System.currentTimeMillis();
            String result=catchUrl(url);
            long end=System.currentTimeMillis();
            System.out.println("====更新数据结果:"+result+"，用时:"+(end-start));
        }
    }

    private static String catchUrl(String url) {
        try {
            URL conn = new URL(url);
            InputStream ins = conn.openStream();
            String result = IOUtils.toString(ins);
            return result;
        } catch (Exception er) {
            throw new RuntimeException(er);
        }
    }
    private static void sleep(long millis){
        try{
            Thread.sleep(millis);
        }catch(Exception er){

        }
    }
}
