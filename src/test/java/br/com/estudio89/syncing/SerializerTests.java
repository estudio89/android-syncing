package br.com.estudio89.syncing;

import br.com.estudio89.syncing.serialization.annotations.JSON;
import br.com.estudio89.syncing.serialization.JSONSerializer;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by luccascorrea on 6/20/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SerializerTests {

    @Test
    public void testSerializer() throws Exception {
        TestClass test = new TestClass();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 20);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        test.setId(1);
        test.setIdServer(2);
        test.setFirstName("Luccas");
        test.setAge(25);
        test.setNickname("Luc");
        test.setDate(cal.getTime());
        test.setAddress("123 My St");
        test.setCity("Curitiba");

        // Testing serialization to json
        JSONSerializer jsonSerializer = new JSONSerializer<TestClass>(TestClass.class);
        JSONObject jsonObject = new JSONObject();
        jsonSerializer.toJSON(test, jsonObject);
        Assert.assertEquals("{\"id\":2,\"address\":\"123 My St\",\"nickname\":\"Luc\",\"date\":\"2015-06-20T13:00:00.000-03:00\",\"user\":{\"myName\":\"Luccas\"},\"clientId\":1,\"city\":\"Curitiba\"}",jsonObject.toString());
        System.out.println(jsonObject.toString());

        test = new TestClass();
        test.setAddress("1234 Your St");
        Date today = new Date();
        test.setDate(today);

        // Testing updating from json
        jsonObject = new JSONObject("{\"id\":2,\"address\":\"123 My St\",\"nickname\":\"Luc\",\"age\":25,\"user\":{\"myName\":\"Luccas\"}, \"clientId\":3, \"date\":\"2015-06-20T13:00:00.000-03:00\",\"city\":\"Curitiba\"}");
        jsonSerializer.updateFromJSON(jsonObject, test);
        Assert.assertEquals(test.getId(), 3);
        Assert.assertEquals(test.getIdServer(), 2);
        Assert.assertEquals(test.getNickname(), "Luc");
        Assert.assertEquals(test.getAge(), 25);
        Assert.assertEquals(test.getFirstName(), "Luccas");
        Assert.assertEquals(test.getCity(), "Curitiba");
        Assert.assertEquals(test.getAddress(), "1234 Your St");
        Assert.assertEquals(test.getDate(), today);

    }

    public static class Parent {

        @JSON(readable = false)
        String address;

        String city;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    public static class TestClass extends Parent {
        @JSON(name="clientId")
        long id;

        @JSON(name="id")
        long idServer;

        @JSON(name="user.myName")
        String firstName;

        @JSON(writable = false)
        int age;

        @JSON(readable = false)
        Date date;

        @JSON(ignoreIf = "-1", readable=false)
        long test=-1;

        String nickname;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getIdServer() {
            return idServer;
        }

        public void setIdServer(long idServer) {
            this.idServer = idServer;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}
