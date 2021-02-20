/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.example;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.installations.InstallationTokenResult;

import ru.iqchannels.sdk.app.Cancellable;
import ru.iqchannels.sdk.app.IQChannels;
import ru.iqchannels.sdk.app.IQChannelsConfig;
import ru.iqchannels.sdk.app.UnreadListener;
import ru.iqchannels.sdk.ui.ChatFragment;

public class IQAppActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, UnreadListener {

    private static final String TAG = "iqchannels-app";
    private Cancellable unread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_app);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        setupIQChannels();
    }

    private void setupIQChannels() {
        FirebaseInstallations.getInstance().getToken(false).addOnCompleteListener(new OnCompleteListener<InstallationTokenResult>() {
            @Override
            public void onComplete(@NonNull Task<InstallationTokenResult> task) {
                if(!task.isSuccessful()){
                    return;
                }

                // Get new Instance ID token.
                String token = task.getResult().getToken();

                IQChannels iq = IQChannels.instance();
                iq.setPushToken(token);
            }
        });

        IQChannels iq = IQChannels.instance();
        // iq.configure(this, new IQChannelsConfig("http://52.57.77.143/", "support"));
        iq.configure(this, new IQChannelsConfig("https://app.iqstore.ru/", "support"));

        iq.loginAnonymous();
        // iq.configure(this, new IQChannelsConfig("http://88.99.143.201/", "support"));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.app, menu);
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment;
        switch (item.getItemId()) {
            case R.id.nav_index:
                fragment = PlusOneFragment.newInstance();
                break;

            case R.id.nav_chat:
                fragment = ChatFragment.newInstance();
                break;

            case R.id.nav_login:
                IQChannels.instance().login("3");
                return false;

            case R.id.nav_logout:
                IQChannels.instance().logout();
                IQChannels.instance().loginAnonymous();
                return false;

            case R.id.nav_logout_anonymous:
                IQChannels.instance().logoutAnonymous();
                return false;

            case R.id.nav_listen_to_unread:
                if (unread == null) {
                    unread = IQChannels.instance().addUnreadListener(this);
                }
                return false;

            case R.id.nav_remove_unread_listener:
                if (unread != null) {
                    unread.cancel();
                    unread = null;
                }

            default:
                return false;
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content, fragment).commit();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void unreadChanged(int unread) {
        Log.i(TAG, String.format("Unread: %d", unread));

        NavigationView nav = findViewById(R.id.nav_view);
        Menu menu = nav.getMenu();
        MenuItem item = menu.findItem(R.id.nav_chat);
        if (unread == 0) {
            item.setTitle("Chat");
        } else {
            item.setTitle(String.format("Chat (%d)", unread));
        }
    }
}
