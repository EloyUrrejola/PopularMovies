package com.example.android.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.example.android.popularmovies.utils.Constants;

public class DetailMovieActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_movie);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailMovieActivityFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // NO SETTINGS BY THE MOMENT
        //getMenuInflater().inflate(R.menu.menu_detail_movie, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailMovieActivityFragment extends Fragment {

        public DetailMovieActivityFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_detail_movie, container, false);

            Intent intent = getActivity().getIntent();
            if (intent != null && intent.hasExtra("movie")) {
                Movie movie = intent.getParcelableExtra("movie");
                // title
                ((TextView) rootView.findViewById(R.id.movie_title))
                        .setText(movie.originalTitle);
                // poster
                ImageView posterImage = (ImageView) rootView.findViewById(R.id.movie_image);
                String url = Constants.URL_PATH + movie.posterPath;
                // Load image with Picasso
                Picasso.with(getActivity())
                        .load(url)
                        .error(R.drawable.poster_default_w342)
                        .into(posterImage);

                // year
                ((TextView) rootView.findViewById(R.id.movie_year))
                        .setText(movie.releaseDate.substring(0, 4));
                // rating
                ((TextView) rootView.findViewById(R.id.movie_rating))
                        .setText(Double.toString(movie.voteAverage) + " / 10");
                // synopsis
                ((TextView) rootView.findViewById(R.id.movie_synopsis))
                        .setText(movie.overview);
            }

            return rootView;
        }
    }
}
