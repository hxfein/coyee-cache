package test.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MqConfig {

    private static final String ROUTING_KEY = "test";

    @Bean
    public DirectExchange testExchange() {
        return new DirectExchange("testExchange", true, false, null);
    }

    @Bean
    public Queue testQueue1() {
        return new Queue("testQueue1", true);
    }

    @Bean
    public Queue testQueue2() {
        return new Queue("testQueue2", true);
    }

    @Bean
    public Queue testQueue3() {
//		Map<String,Object> args=new HashMap<>();
//		args.put("x-message-ttl",1000);
//		args.put("x-dead-letter-routing-key","test");
//		args.put("x-dead-letter-exchange","testExchange");
        Queue queue = new Queue("testQueue3", true, true, true);
        return queue;
    }

    @Bean
    public Binding bindingUserEventMessage1() {
        return BindingBuilder
                .bind(testQueue1())
                .to(testExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding bindingUserEventMessage2() {
        return BindingBuilder
                .bind(testQueue2())
                .to(testExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange("delayExchange", "x-delayed-message", false, false, args);
    }

    @Bean
    public Binding bindingUserEventMessage3() {
        return BindingBuilder.bind(testQueue3()).to(delayExchange()).with("none").noargs();
    }

}