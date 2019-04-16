package mechanic.local.kumar.ayush.localmechanic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MarkerDetails extends AppCompatActivity
{
    String lat , lng , name , rating ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_details);

        if (getIntent() != null)
        {
            lat = getIntent().getStringExtra("lat");
            lng = getIntent().getStringExtra("lng");
         //   name = getIntent().getStringExtra("name");
         //   rating = getIntent().getStringExtra("rating");
        }

        Toast.makeText(MarkerDetails.this , lat + " " +lng  , Toast.LENGTH_SHORT).show();
    }
}
