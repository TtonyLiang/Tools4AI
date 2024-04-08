package com.t4a.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.t4a.examples.ArrayAction;
import com.t4a.examples.actions.Customer;
import com.t4a.examples.actions.ListAction;
import com.t4a.examples.actions.PlayerWithRestaurant;
import com.t4a.examples.basic.DateDeserializer;
import com.t4a.examples.basic.RestaurantPojo;
import com.t4a.examples.pojo.Organization;
import com.t4a.predict.OpenAIPromptTransformer;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.OpenAiActionProcessor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
@Slf4j
public class OpenAIActionTest {
    @Test
    public void testRestaurantPojoOpenAI() throws AIProcessingException {
        String promptText = "can you book a dinner reseration in name of Vishal and his family of 4 at Maharaj restaurant in Toronto, on Indian Independence day and make sure its cancellable";
        OpenAIPromptTransformer tools = new OpenAIPromptTransformer();
        RestaurantPojo pojo = (RestaurantPojo) tools.transformIntoPojo(promptText, "com.t4a.examples.basic.RestaurantPojo", "RestaurantClass", "Create Pojo from the prompt");
        Assertions.assertTrue(pojo != null);
        Assertions.assertEquals(pojo.getName(), "Vishal");
        Assertions.assertEquals(pojo.getNumberOfPeople(), 4);
        Assertions.assertTrue(pojo.getRestaurantDetails().getName().contains("Maharaj"));
        Assertions.assertEquals(pojo.getRestaurantDetails().getLocation(), "Toronto");

    }

    @Test
    public void testCustomerPojoOpenAI() throws AIProcessingException, IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer("dd MMMM yyyy"));
        Gson gson = gsonBuilder.create();
        OpenAIPromptTransformer tools2 = new OpenAIPromptTransformer(gson);

        Customer pojo = (Customer) tools2.transformIntoPojo("I went to the part yesterday and met someone it was so good to meet an old friend. A customer is complaining that his computer is not working, his name is Vinod Gupta,  and he stays in Toronto he joined on 12 May 2008", Customer.class.getName(),"Customer", "get Customer details");
        Assertions.assertTrue(pojo != null);
         String reasonMatches = TestHelperOpenAI.getInstance().sendMessage("reply in true or false only - is this "+pojo.getReasonForCalling()+" same as computer not working");
         Assertions.assertTrue("True".equalsIgnoreCase(reasonMatches));
        Assertions.assertEquals(pojo.getFirstName(),"Vinod");
        Assertions.assertEquals(pojo.getLastName(),"Gupta");
    }

    @Test
    public void testComplexActionOpenAI() throws AIProcessingException, IOException {
        OpenAiActionProcessor processor = new OpenAiActionProcessor();
        String prm = "Sachin Tendulkar is a cricket player and he has played 400 matches, his max score is 1000, he wants to go to " +
                "Maharaja restaurant in toronto with 4 of his friends on Indian Independence Day, can you notify him and the restarurant";
        PlayerWithRestaurant playerAc = new PlayerWithRestaurant();
        String result = (String)processor.processSingleAction(prm,playerAc);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(playerAc.getRestaurantPojo());
        Assertions.assertNotNull(playerAc.getPlayer());
        Assertions.assertEquals(playerAc.getPlayer().getFirstName(),"Sachin");
        Assertions.assertEquals(playerAc.getPlayer().getLastName(),"Tendulkar");
    }

    @Test
    public void testHttpActionOpenAI() throws AIProcessingException, IOException {
        OpenAiActionProcessor processor = new OpenAiActionProcessor();
        String postABook = "post a book harry poster with id 189 the publish date is 2024-03-22 and the description is about harry who likes poster its around 500 pages  ";
        String result = (String)processor.processSingleAction(postABook);
        Assertions.assertNotNull(result);
        String success = TestHelperOpenAI.getInstance().sendMessage("Look at this message - "+result+" - was it a success? - Reply in true or false only");
        log.debug(success);
        Assertions.assertTrue("True".equalsIgnoreCase(success));

    }

    @Test
    public void testShellActionOpenAI() throws AIProcessingException, IOException {
        OpenAiActionProcessor processor = new OpenAiActionProcessor();
        String shellAction = "An Employee joined the organization, his name is Vishal and his location is Toronto, save this information ";
        String result = (String)processor.processSingleAction(shellAction);
        Assertions.assertNotNull(result);
        String success = TestHelperOpenAI.getInstance().sendMessage("Look at this message - "+result+" - was it a success? - Reply in true or false only");
        log.debug(success);
        Assertions.assertTrue("True".equalsIgnoreCase(success));
    }

    @Test
    public void testJavaMethod() throws AIProcessingException, IOException {
        OpenAiActionProcessor processor = new OpenAiActionProcessor();
        String weatherAction = "ey I am in Toronto do you think i can go out without jacket";
        String result = (String)processor.processSingleAction(weatherAction);
        Assertions.assertNotNull(result);
        String success = TestHelperOpenAI.getInstance().sendMessage("Look at this message - "+result+" - was it a success? - Reply in true or false only");
        log.debug(success);
        Assertions.assertTrue("True".equalsIgnoreCase(success));
    }
    @Test
    public void testJavaMethodForFile() throws AIProcessingException, IOException {
        OpenAiActionProcessor processor = new OpenAiActionProcessor();
        String weatherAction = "My friends name is Vishal ,he lives in toronto.I want save this info locally";
        String result = (String)processor.processSingleAction(weatherAction);
        Assertions.assertNotNull(result);
        String success = TestHelperOpenAI.getInstance().sendMessage("Look at this message - "+result+" - was it a success? - Reply in true or false only");
        log.debug(success);
        Assertions.assertTrue("True".equalsIgnoreCase(success));
    }
    @Test
    public void testActionWithList() throws AIProcessingException, IOException {
        OpenAiActionProcessor processor = new OpenAiActionProcessor();
        String promptText = "Shahrukh Khan works for MovieHits inc and his salary is $ 100  he joined Toronto on Labor day, his tasks are acting and dancing. He also works out of Montreal and Bombay.Hrithik roshan is another employee of same company based in Chennai his taks are jumping and Gym he joined on Indian Independce Day";
        ListAction action = new ListAction();
        Organization org = (Organization) processor.processSingleAction(promptText,action);
        Assertions.assertTrue(org.getEm().get(0).getName().contains("Shahrukh"));
        Assertions.assertTrue(org.getEm().get(1).getName().contains("Hrithik"));
    }

    @Test
    public void testActionWithListAndArray() throws AIProcessingException, IOException {
        OpenAiActionProcessor processor = new OpenAiActionProcessor();
        String promptText = "Shahrukh Khan works for MovieHits inc and his salary is $ 100  he joined Toronto on Labor day, his tasks are acting and dancing. He also works out of Montreal and Bombay.Hrithik roshan is another employee of MovieHits inc based in Chennai his taks are jumping and Gym he joined on Indian Independce Day.Vishal Mysore is customer of MovieHits inc and he styas in Paris the reason he is not happy is that he did nto like the movie date is labor day. Deepak Rao is another customer for MovieHits inc date is independence day";
        ListAction action = new ListAction();
        Organization org = (Organization) processor.processSingleAction(promptText,action);
        Assertions.assertTrue(org.getEm().get(0).getName().contains("Shahrukh"));
        Assertions.assertTrue(org.getEm().get(1).getName().contains("Hrithik"));
    }
    @Test
    public void testActionWithArray() throws AIProcessingException, IOException {
        OpenAiActionProcessor processor = new OpenAiActionProcessor();
        String promptText = "Vishal Mysore is customer of MovieHits inc and he styas in Paris the reason he is not happy is that he did nto like the movie date is labor day. Deepak Rao is another customer for MovieHits inc date is independence day";
        ArrayAction action = new ArrayAction();
        Customer[] org = (Customer[]) processor.processSingleAction(promptText,action);
        Assertions.assertTrue(org.length == 2);


    }
    @Test
    public void testPojoWithList() throws AIProcessingException, IOException {
        OpenAIPromptTransformer tra = new OpenAIPromptTransformer();
        String promptText = "Shahrukh Khan works for MovieHits inc and his salary is $ 100  he joined Toronto on Labor day, his tasks are acting and dancing. He also works out of Montreal and Bombay. Hrithik roshan is another employee of same company based in Chennai his taks are jumping and Gym he joined on Indian Independce Day";
        Organization org = (Organization) tra.transformIntoPojo(promptText, Organization.class.getName(),"","");
        Assertions.assertTrue(org.getEm().get(0).getName().contains("Shahrukh"));
        Assertions.assertTrue(org.getEm().get(1).getName().contains("Hrithik"));
    }



    @Test
    public void testHighRiskAction() throws AIProcessingException, IOException {
        OpenAiActionProcessor processor = new OpenAiActionProcessor();
        String ecomActionPrmt = "Hey This is Vishal, the ecommerce Server is very slow and users are not able to do online shopping";
        String result = (String)processor.processSingleAction(ecomActionPrmt);
        log.info(result);
        Assertions.assertNotNull(result);
        String success = TestHelperOpenAI.getInstance().sendMessage("Look at this message - "+result+" - was it a success? - Reply in true or false only");
        log.debug(success);
        Assertions.assertTrue("false".equalsIgnoreCase(success));
        result = (String)processor.processSingleAction(ecomActionPrmt,"restartTheECOMServer");
        log.info(result);
        Assertions.assertNotNull(result);
        success = TestHelperOpenAI.getInstance().sendMessage("Look at this message - "+result+" - was it a success? - Reply in true or false only");
        log.debug(success);
        Assertions.assertTrue("true".equalsIgnoreCase(success));
    }
}