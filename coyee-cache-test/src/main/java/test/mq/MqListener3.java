package test.mq;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


/**
 * @author hxfein
 * @className: MqListener
 * @description:
 * @date 2022/8/19 16:22
 * @version：1.0
 */
@Component
public class MqListener3 {

    @RabbitListener(queues = "testQueue3")
    @RabbitHandler
    public void process(String str) {
        System.out.println("delaydelaydelay:"+str);
    }
}
