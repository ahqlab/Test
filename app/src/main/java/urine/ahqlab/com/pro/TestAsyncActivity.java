package urine.ahqlab.com.pro;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class TestAsyncActivity extends Activity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AbstractAsynTask2 task2 =  new AbstractAsynTask2<String>(){
            @Override
            protected String getDoing() {
                try {
                    for (int i = 0; i < 5; i++) {
                        //asyncDialog.setProgress(i * 30);
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "123456";
            }

            @Override
            protected void getFinish(String d) {
                Log.e("HJLEE : ", d);
            }
        };
        task2.new TestAsynTask().execute();
    }
}

