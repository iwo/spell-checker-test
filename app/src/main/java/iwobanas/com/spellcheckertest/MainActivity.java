package iwobanas.com.spellcheckertest;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SpellCheckerTest";

    private final int TEST_LIMIT = 10000;
    private int testIndex = 0;
    private Locale[] locales = new Locale[]{
            new Locale("en", "US"),
            new Locale("es", "ES"),
            new Locale("pl", "PL"),
            new Locale("ru", "RU")
    };
    private TestFileAsyncTask currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startNextTest();
    }

    @Override
    protected void onDestroy() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        super.onDestroy();
    }

    private void startNextTest() {
        if (testIndex < locales.length) {
            currentTask = new TestFileAsyncTask(locales[testIndex++]);
            currentTask.execute();
        } else {
            Log.v(TAG, "All tests finished");
        }
    }

    private class TestFileAsyncTask extends AsyncTask<Void, String, Void> {

        private final String localeCode;
        private SpellCheckerSession session;

        private Object lock = new Object();

        private volatile boolean wordRecognized;
        private SpellCheckerSession.SpellCheckerSessionListener listener = new SpellCheckerSession.SpellCheckerSessionListener() {
            @Override
            public void onGetSuggestions(SuggestionsInfo[] suggestions) {
                synchronized (lock) {
                    wordRecognized = ((suggestions[0].getSuggestionsAttributes() & SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) != 0);
                    lock.notify();
                }
            }

            @Override
            public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfos) {
            }
        };

        private BufferedReader reader;
        private BufferedReader countReader;
        private BufferedWriter recognizedWriter;
        private BufferedWriter unrecognizedWriter;
        private BufferedWriter reportWriter;

        public TestFileAsyncTask(Locale locale) {
            localeCode = locale.getLanguage() + "_" + locale.getCountry();
            TextServicesManager textServicesManager = (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);
            session = textServicesManager.newSpellCheckerSession(null, locale, listener, false);
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                if (session == null) {
                    Log.w(TAG, "Spell Checker Session not created");
                }
                Log.v(TAG, "Selected Spell Checker:" + session.getSpellChecker().getComponent());

                openFiles();

                int count = getCount();
                Log.v(TAG, localeCode + " " + count + " words");
                int recognized = 0;
                int unrecognized = 0;

                double sampleRatio = 1.1 * TEST_LIMIT / count; // multiply by 1.1 to increase a chance that we'll get at least TEST_LIMIT tests

                String line;
                while (!isCancelled() && (recognized + unrecognized) <= TEST_LIMIT
                        && (line = reader.readLine()) != null) {
                    if (sampleRatio < 1.0 && Math.random() > sampleRatio)
                        continue;

                    synchronized (lock) {
                        session.getSuggestions(new TextInfo(line), 0);
                        lock.wait(); // wait for the notification from listener
                    }
                    if (wordRecognized) {
                        recognized++;
                        recognizedWriter.write(line);
                        recognizedWriter.write("\n");
                    } else {
                        unrecognized++;
                        unrecognizedWriter.write(line);
                        unrecognizedWriter.write("\n");
                    }
                    if ((recognized + unrecognized) % 100 == 0) {
                        Log.v(TAG, localeCode + " recognized: " + recognized + " unrecognized: " + unrecognized + "   " + (100 * recognized) / (recognized + unrecognized) + " %");
                    }
                }
                reportWriter.write(localeCode + "," + count + "," + recognized + "," + unrecognized + ",\n");

            } catch (IOException | InterruptedException e) {
                Log.w(TAG, e);
            } finally {
                closeFiles();
            }
            return null;
        }

        private int getCount() throws IOException {
            int count = 0;
            while (!isCancelled() && countReader.readLine() != null) {
                count++;
            }
            return count;
        }

        private void openFiles() throws FileNotFoundException {
            File inputFile = new File(Environment.getExternalStorageDirectory(), "hunspell_words_" + localeCode + ".txt");
            File recognizedFile = new File(Environment.getExternalStorageDirectory(), "recognized_" + localeCode + ".txt");
            File unrecognizedFile = new File(Environment.getExternalStorageDirectory(), "unrecognized_" + localeCode + ".txt");
            File reportFile = new File(Environment.getExternalStorageDirectory(), "spell_checker_test.csv");

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
            countReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
            recognizedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(recognizedFile)));
            unrecognizedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(unrecognizedFile)));
            reportWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(reportFile, true)));
        }

        private void closeFiles() {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
            if (countReader != null) {
                try {
                    countReader.close();
                } catch (IOException ignore) {
                }
            }
            if (recognizedWriter != null) {
                try {
                    recognizedWriter.close();
                } catch (IOException ignore) {
                }
            }
            if (unrecognizedWriter != null) {
                try {
                    unrecognizedWriter.close();
                } catch (IOException ignore) {
                }
            }
            if (reportWriter != null) {
                try {
                    reportWriter.close();
                } catch (IOException ignore) {
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            startNextTest();
        }
    }
}
