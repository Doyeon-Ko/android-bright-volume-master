package com.example.brightvolumemaster;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.View; //for handling button clicks
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ViewPagerAdapter adapter;

    private RelativeLayout pageLayout;

    int screenNum = 0;

    private int screenWidth;

    private final int columnCount = 3;

    private final int rowCount = 6;

    private final int appsPerPage = 9;

    private int pixelValue;

    // Calculate 10% of the screen width as the gap
    private int gap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        float gapPercentage = 0.1f;
        gap = (int) (screenWidth * gapPercentage);

        // Get the screen density
        float scale = getResources().getDisplayMetrics().density;
        int dpValue = 20;
        pixelValue = (int) (dpValue * scale + 0.5f);

        // Initialize UI elements
        ViewPager viewPager = findViewById(R.id.viewPager);
        adapter = new ViewPagerAdapter();

        // Get all installed apps
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> installedApps = getInstalledApps(packageManager);

        int rowIndex = 0;
        int columnIndex = 0;
        int appNum = 0;

        Set<String> uniqueAppPackageNames = new HashSet<>();

        for (int i = 0; i < installedApps.size(); i++) {
            ResolveInfo appInfo = installedApps.get(i);
            String packageName = appInfo.activityInfo.packageName;

            if (!uniqueAppPackageNames.contains(packageName)) {
                uniqueAppPackageNames.add(packageName);
                int page = (appNum / appsPerPage) + 1;
                appNum++;

                if (appNum == 1 && page == 1) {
                    pageLayout = createPageLayout();
                }

                loadAppInfo(pageLayout, appInfo, rowIndex, columnIndex);
                columnIndex++;

                if (appNum % appsPerPage == 0) {
                    // If current page is filled with nine apps, new page should be added.

                    if (appNum == appsPerPage) {
                        // Set up the ViewPager with the adapter
                        viewPager.setAdapter(adapter);
                        viewPager.addOnPageChangeListener(adapter);
                    }

                    addNewScreen(pageLayout);
                    ++screenNum;

                    pageLayout = createPageLayout();
                    rowIndex = 0;
                    columnIndex = 0;
                    continue;
                }

                if (columnIndex == columnCount) {
                    // Move to the next row
                    columnIndex = 0;
                    rowIndex++;
                }
            }
        }

        // Save uniqueAppPackageNames to SharedPreferences
        SharedPreferences preferences = getSharedPreferences("UserApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("uniqueAppPackageNames", uniqueAppPackageNames);
        editor.apply();

        if (appNum % appsPerPage != 0 && appNum > 0) {
            // If there is a screen containing less than nine apps, we should display the addedGridLayout as well, although it isn't completely filled with nine apps.
            if (appNum < appsPerPage) {
                viewPager.setAdapter(adapter);
                viewPager.addOnPageChangeListener(adapter);
            }
            addNewScreen(pageLayout);
            ++screenNum;
        }
    }

    private List<ResolveInfo> getInstalledApps(PackageManager packageManager) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        return packageManager.queryIntentActivities(mainIntent, 0);
    }

    private LinearLayout createLinearLayoutForApp(int columnCount, int linearLayoutNum) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setPadding(0, 20, 0, 20);

        GridLayout.LayoutParams linearlayoutParams = new GridLayout.LayoutParams();
        linearlayoutParams.width = 0;
        linearlayoutParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        linearlayoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); //Equal column weight
        linearlayoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED,1f);
        linearlayoutParams.leftMargin = gap;

        if (linearLayoutNum > columnCount - 1) {
            linearlayoutParams.topMargin = pixelValue;
        }

        linearLayout.setLayoutParams(linearlayoutParams);

        // Create ImageButton(Space for App Icon)
        ImageButton appIcon = new ImageButton(this);

        // Calculate the ImageButton size based on the screen width
        int spacing = gap * (columnCount + 1);
        int imageSize = (screenWidth - spacing) / columnCount;
        LinearLayout.LayoutParams appIconParams = new LinearLayout.LayoutParams(imageSize, imageSize);
        appIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        appIcon.setLayoutParams(appIconParams);
        appIcon.setBackgroundColor(Color.TRANSPARENT);

        //Create TextView
        TextView appName = new TextView(this);
        appName.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        appName.setTextSize(14);
        appName.setTypeface(null, Typeface.BOLD);
        appName.setGravity(Gravity.CENTER);

        appName.setMaxLines(2);
        appName.setEllipsize(TextUtils.TruncateAt.END);
        appName.setMaxWidth(imageSize);

        // Add the ImageButton and TextView to the LinearLayout
        linearLayout.addView(appIcon);
        linearLayout.addView(appName);

        return linearLayout;
    }

    private GridLayout createGridLayoutForApps(int columnCount, int rowCount) {
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(columnCount);
        gridLayout.setRowCount(rowCount);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_MARGINS);

        // Set layout parameters for GridLayout
        RelativeLayout.LayoutParams gridLayoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        gridLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        gridLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        gridLayout.setLayoutParams(gridLayoutParams);

        // Set margins for the GridLayout
        int marginTop = getResources().getDimensionPixelSize(R.dimen.grid_layout_margin_top);
        int marginBottom = getResources().getDimensionPixelSize(R.dimen.grid_layout_margin_bottom);

        gridLayoutParams.setMargins(0, marginTop, 0, marginBottom);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_MARGINS);

        // Add LinearLayouts (each containing appIcon and appName) to GridLayout
        for (int i = 0; i < appsPerPage; i++) {
            LinearLayout linearLayoutForApp = createLinearLayoutForApp(columnCount, i);
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();

            // Set layout parameters for the LinearLayout
            layoutParams.width = GridLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f);
            layoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL,1f);
            linearLayoutForApp.setLayoutParams(layoutParams);

            // Add the LinearLayout to the GridLayout
            gridLayout.addView(linearLayoutForApp);
        }

        return gridLayout;
    }

    private RelativeLayout createPageLayout(){
        RelativeLayout relativeLayout = new RelativeLayout(this);

        TextView newTextView = new TextView(this);
        newTextView.setText(R.string.choose_your_app);
        newTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        newTextView.setTextColor(ContextCompat.getColor(this, R.color.white));
        newTextView.setTypeface(null, Typeface.BOLD);

        // Set layout parameters for TextView
        RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);

        int textBoxMarginTop = (int) getResources().getDimension(R.dimen.text_box_margin_top);
        textViewParams.setMargins(0, textBoxMarginTop, 0, 0);

        relativeLayout.addView(newTextView, textViewParams);

        // Create GridLayout for apps
        GridLayout gridLayout = createGridLayoutForApps(columnCount, rowCount);

        // Set layout parameters for the GridLayout
        RelativeLayout.LayoutParams gridLayoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        // Position the GridLayout below the TextBox
        gridLayoutParams.addRule(RelativeLayout.BELOW, newTextView.getId());
        gridLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        gridLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        gridLayout.setLayoutParams(gridLayoutParams);

        // Add the GridLayout to the RelativeLayout
        relativeLayout.addView(gridLayout);

        return relativeLayout;
    }

    private void loadAppInfo(RelativeLayout relativeLayout, final ResolveInfo appInfo, int rowIndex, int columnIndex) {
        // Find the GridLayout dynamically
        GridLayout gridLayout = null;
        for (int i = 0; i < relativeLayout.getChildCount(); i++) {
            View childView = relativeLayout.getChildAt(i);
            if (childView instanceof GridLayout) {
                gridLayout = (GridLayout) childView;
                break;
            }
        }

        if (gridLayout == null) {
            return;
        }

        // Find the LinearLayout at the specified rowIndex and columnIndex
        int childIndex = rowIndex * columnCount + columnIndex;
        LinearLayout linearLayout = (LinearLayout) gridLayout.getChildAt(childIndex);

        // Extract ImageButton and TextView from the LinearLayout
        ImageButton appIcon = (ImageButton) linearLayout.getChildAt(0);
        TextView appName = (TextView) linearLayout.getChildAt(1);

        // Load actual app icon and app name into the ImageButton and TextView
        Drawable iconDrawable = appInfo.loadIcon(getPackageManager());
        appIcon.setImageDrawable(iconDrawable);
        appIcon.setContentDescription(appInfo.loadLabel(getPackageManager()).toString());

        appName.setText(appInfo.loadLabel(getPackageManager()));

        // Set onClickListener for the app icon
        appIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // This code defines what happens when an app icon is clicked
                // It doesn't execute immediately; it waits for a click event
                startSubActivity(appInfo);
            }
        });
    }

    private void addNewScreen(RelativeLayout createdPageLayout) {
        adapter.addPageLayout(createdPageLayout);
    }

    private void startSubActivity(ResolveInfo appInfo) {
        Intent subActivityIntent = new Intent(MainActivity.this, SubActivity.class);

        //Pass necessary data to the SubActivity : selected app name and icon
        subActivityIntent.putExtra("SELECTED_APP_NAME", appInfo.loadLabel(getPackageManager()).toString());
        subActivityIntent.putExtra("PACKAGE_NAME", appInfo.activityInfo.packageName);

        //Convert the Drawable to a byte array
        byte[] iconByteArray = getByteArrayFromDrawable(appInfo.loadIcon(getPackageManager()));
        subActivityIntent.putExtra("SELECTED_APP_ICON", iconByteArray);

        startActivity(subActivityIntent);
    }

    // Convert Drawable to byte array
    private byte[] getByteArrayFromDrawable(Drawable drawable) {
        Bitmap bitmap = drawableToBitmap(drawable);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    // Convert Drawable to Bitmap
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static class ViewPagerAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener{

        private final List<RelativeLayout> pageLayouts = new ArrayList<>();

        public void addPageLayout(RelativeLayout layout) {
            pageLayouts.add(layout);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return pageLayouts.size();
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            // Call the superclass method to handle default instantiation logic
            RelativeLayout layout = pageLayouts.get(position);
            container.addView(layout);

            return layout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            container.removeView((RelativeLayout) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        // Implementing onPageScrolled
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        // Implementing onPageSelected
        @Override
        public void onPageSelected(int position) {

        }

        //Implementing onPageScrollStateChanged
        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}