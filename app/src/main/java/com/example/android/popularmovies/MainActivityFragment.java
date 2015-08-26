package com.example.android.popularmovies;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.example.android.popularmovies.utils.Constants;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    // API KEY for themoviedb.org
    private final String MDB_APIKEY = "";

    private List<Movie> movies;

    private MovieAdapter movieAdapter;

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        movies = new ArrayList<>();

        if (savedInstanceState != null){
            Log.i("kkk", "pasa");
            //get back your data and populate the adapter
            movies = (List<Movie>) savedInstanceState.get("MOVIES_KEY");
        } else {
            updateMovies();
        }
        movieAdapter = new MovieAdapter(getActivity(), movies);

        GridView gridView = (GridView) rootView.findViewById(R.id.gridview_movies);
        gridView.setAdapter(movieAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Movie movie = (Movie) movieAdapter.getItem(i);
                Intent intent = new Intent(getActivity(), DetailMovieActivity.class)
                        .putExtra("movie", movie);
                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("MOVIES_KEY", (ArrayList<? extends Parcelable>) movies);
    }

    private void updateMovies() {
        // Get order preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String order = prefs.getString(getString(R.string.pref_order_key),
                getString(R.string.pref_order_default));
        FetchMoviesTask moviesTask = new FetchMoviesTask();
        moviesTask.execute(order);
    }

    @Override
    public void onStart() {
        // update movies on start
        Log.i("kkk", "onStart");
        super.onStart();
        //updateMovies();
    }

    private class MovieAdapter extends ArrayAdapter<Movie> {

        public MovieAdapter(Context context, List<Movie> movies) {
            super(context, R.layout.grid_item_movies, movies);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){

            //if the view is null than inflate it otherwise just fill the list with
            if (convertView == null){
                //inflate the layout
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.grid_item_movies, parent, false);
            }
            ImageView imageView = (ImageView) convertView.findViewById(R.id.grid_item_movies_imageview);

            String url = Constants.URL_PATH + movies.get(position).posterPath;

            // Load image with Picasso
            Picasso.with(getActivity())
                    .load(url)
                    .error(R.drawable.poster_default_w342)
                    .into(imageView);

            return  convertView;
        }
    }

    public class FetchMoviesTask extends AsyncTask<String, Void, List<Movie>> {

        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        @Override
        protected List<Movie> doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String moviesJsonStr = null;

            List<Movie> movieList = new ArrayList<>();

            try {
                // Construct the URL for the themoviedb query
                // http://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&api_key=064ac7c853c437349eeb0676e2444156

                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("api.themoviedb.org")
                        .appendPath("3")
                        .appendPath("discover")
                        .appendPath("movie")
                        .appendQueryParameter("sort_by", params[0])
                        .appendQueryParameter("api_key", MDB_APIKEY)
                        .build();
                URL url = new URL(builder.toString());

                // Create the request to themoviedb, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    moviesJsonStr = null;
                }
                moviesJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movies data, there's no point in attempting
                // to parse it.
                moviesJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            ///////////////////////////////////////////////

            try {
                movieList = getMoviesFromJson(moviesJsonStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return movieList;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private List<Movie> getMoviesFromJson(String moviesJsonStr)
                throws JSONException {

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray moviesArray = moviesJson.getJSONArray("results");

            List<Movie> movieList = new ArrayList<>();

            for (int i = 0; i < moviesArray.length(); i++) {
                // Get the JSON object representing the movie
                JSONObject movieJSON = moviesArray.getJSONObject(i);
                Movie movie = new Movie();
                // Get to movie poster and add it to the list
                movie.id = movieJSON.getInt("id");
                movie.originalTitle = movieJSON.getString("original_title");
                movie.posterPath = movieJSON.getString("poster_path");
                movie.overview = movieJSON.getString("overview");
                movie.releaseDate = movieJSON.getString("release_date");
                movie.voteAverage = movieJSON.getDouble("vote_average");

                movieList.add(movie);
            }

            return movieList;
        }

        @Override
        protected void onPostExecute(List<Movie> movies) {
            if (movies != null) {
                // It moves the movie list to the ArrayAdapter
                movieAdapter.clear();
                movieAdapter.addAll(movies);
            }
        }
    }
}
