package test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author hxfein
 * @className: Test
 * @description:
 * @date 2022/5/5 13:51
 * @version：1.0
 */
public class Test {
    public String getRentId(){
        return "rentId";
    }
    public String getName(){
        return "hello";
    }
    public static void main(String[] args) {
        SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, Test.class.getClassLoader());
        ExpressionParser expressionParser = new SpelExpressionParser(configuration);
        EvaluationContext context = new StandardEvaluationContext();
        context.setVariable("bean",new Test());

        Object s=expressionParser.parseExpression("#bean.rentId+#bean.name").getValue(context);
        System.out.println(s);
    }
}
