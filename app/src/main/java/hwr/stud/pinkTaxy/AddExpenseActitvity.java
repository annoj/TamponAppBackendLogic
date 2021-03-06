package hwr.stud.pinkTaxy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import hwr.stud.mylibrary.HttpBasicAuth;
import hwr.stud.mylibrary.HttpsPostRequest;
import hwr.stud.mylibrary.HttpsUtility;

import static hwr.stud.mylibrary.HttpsConnection.getConnection;

public class AddExpenseActitvity extends AppCompatActivity {

    private static final int REQUEST_CAPTURE_IMAGE = 100;

    Button addExpense;
    Button takeFoto;

    EditText expenseValue;
    EditText expenseArticle;

    Intent privateStats;

    ImageView expenseImageView;

    Bitmap imageBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense_acitvity);



        expenseArticle = (EditText) findViewById(R.id.expenseArticle);
        expenseArticle.setVisibility(View.INVISIBLE);

        expenseValue = (EditText) findViewById(R.id.expenseValue);
        expenseValue.setVisibility(View.INVISIBLE);

        addExpense = (Button) findViewById(R.id.addExpense);
        addExpense.setVisibility(View.INVISIBLE);
        privateStats = new Intent(this, PrivateStatsActivity.class);

        expenseImageView = (ImageView) findViewById(R.id.expenseImage);
        expenseImageView.setVisibility(View.INVISIBLE);

        takeFoto = (Button) findViewById(R.id.takeFoto);

        takeFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openCameraIntent();

                // startActivity(privateStats);
            }

            private void openCameraIntent() {
                Intent pictureIntent = new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE
                );
                if(pictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(pictureIntent,
                            REQUEST_CAPTURE_IMAGE);
                }
            }

        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i("[onActivityResult]", "was entered.");

        addExpense.setVisibility(View.VISIBLE);

        if (requestCode == REQUEST_CAPTURE_IMAGE &&
                resultCode == RESULT_OK) {
            Log.i("[onActivityResult]", "ok.");

            if (data != null && data.getExtras() != null) {
                Log.i("[data != null]", "entered");
                imageBitmap = (Bitmap) data.getExtras().get("data");

                expenseValue.setVisibility(View.VISIBLE);
                expenseArticle.setVisibility(View.VISIBLE);

                expenseImageView.setVisibility(View.VISIBLE);
                expenseImageView.setImageBitmap(imageBitmap);
                Log.i("[expenseImageView]", Integer.toString(imageBitmap.getByteCount()));
                Log.i("[expenseImageView]", "was set.");

                addExpense.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        if(imageBitmap != null) {

                            JSONObject requestBody = new JSONObject();


                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {

                                    HttpsUtility.trustAllCertificates();

                                    try {
                                        requestBody.put("imageName", Long.toString(System.currentTimeMillis()));
                                        requestBody.put("file", imageBitmap);
                                        requestBody.put("value", expenseValue.getText());
                                        requestBody.put("article", expenseArticle.getText());
                                        Log.i("[requestBody]", requestBody.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                       /*             privateStats.putExtra("date", Long.toString(System.currentTimeMillis()));
                                    privateStats.putExtra("value", expenseValue.getText());
                                    privateStats.putExtra("description", expenseArticle.getText());*/

                                    URL url = null;
                                    try {
                                        url = new URL("https://192.168.178.54:443/upload");
                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    }

                                    HttpsURLConnection connection = null;
                                    SharedPreferences pref = getApplicationContext().getSharedPreferences("Creds", 0); // 0 - for private mode
                                    Log.i("[Shared Preferences]", pref.getString("password", null));
                                    Log.i("[Shared Preferences]", pref.getString("username", null));

                                    HttpsUtility.trustAllCertificates();

                                    try {
                                        connection = getConnection(url, new HttpBasicAuth().getAuthString(pref.getString("username", null), pref.getString("password", null)));
                                        HttpsPostRequest.sendRequest(connection, requestBody);
                                        Log.i("[HttpsPostRequest]", "was sendt.");

                                        // handle response as json
                                        if (connection.getResponseCode() == 200) {
                                            InputStream responseBody = connection.getInputStream();
                                            InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                                            JsonReader jsonReader = new JsonReader(responseBodyReader);

                                            // check for  success
                                            jsonReader.setLenient(true);
                                            jsonReader.beginObject();
                                            while (jsonReader.hasNext()) {
                                                String key = jsonReader.nextName();
                                                if (key.equals("success")) {
                                                    if (jsonReader.nextString().equals("true")) {
                                                        startActivity(privateStats);
                                                    }
                                                } else {
                                                    jsonReader.skipValue();
                                                }
                                            }
                                            jsonReader.endObject();
                                            Log.i("[jsonReader]", jsonReader.toString());
                                            jsonReader.close();
                                        }
                                        connection.disconnect();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }

                            });


                        }
                    }
                });

            }
        }
    }
}
