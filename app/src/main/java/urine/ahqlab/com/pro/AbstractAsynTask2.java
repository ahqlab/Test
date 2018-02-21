package urine.ahqlab.com.pro;

import android.os.AsyncTask;

/**
 * Created by silve on 2018-02-09.
 */

public abstract class AbstractAsynTask2<T>{

    protected abstract T getDoing();

    protected abstract void getFinish(T d);

    public class TestAsynTask extends AsyncTask<Void, Void, T> {

        @Override
        protected T doInBackground(Void... voids) {
            return (T) getDoing();
        }

        @Override
        protected void onPostExecute(T d) {
            getFinish(d);
            super.onPostExecute(d);
        }
    }
}
