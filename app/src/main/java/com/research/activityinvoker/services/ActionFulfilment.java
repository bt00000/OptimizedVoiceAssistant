package com.research.activityinvoker.services;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.research.activityinvoker.JsonApi;
import com.research.activityinvoker.R;
import com.research.activityinvoker.SettingsActivity;
import com.research.activityinvoker.model.LabelFoundNode;
import com.research.activityinvoker.model.PackageDataObject;
import com.research.activityinvoker.model.TooltipRequiredNode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


//add new
import java.util.HashMap;
import android.os.Looper;
import android.view.KeyEvent;
import java.io.IOException;
import android.net.Uri;
import android.provider.MediaStore;

import android.media.MediaPlayer;


//end

@RequiresApi(api = Build.VERSION_CODES.R)
public class ActionFulfilment extends AccessibilityService implements View.OnTouchListener {

    final String FILE_NAME = "voicify";
    final String ALL_COMMANDS = "all_commands";
    int width, height;
    Button listenButton;
    //TextView textMsg;
    TextView inputTxt;
    Retrofit retrofit;
    JsonApi jsonApi;

    ArrayList<String> outputArr = new ArrayList<>();
    ArrayList<AccessibilityNodeInfo> editableNodes = new ArrayList<>();
    ArrayList<String> uiElements = new ArrayList<String>();
    ArrayList<String> previousUIElements = new ArrayList<String>();
    ArrayList<String> appNames = new ArrayList<String>();
    SharedPreferences mPrefs;
    SharedPreferences sharedPreferences;
    ArrayList<String> predefinedCommands = new ArrayList<>();
    ArrayList<String> currentSequence = new ArrayList<String>();
    AccessibilityNodeInfo currentSource;
    AccessibilityNodeInfo previousSource;
    ArrayList<AccessibilityNodeInfo> scrollableNodes = new ArrayList<AccessibilityNodeInfo>();

    FrameLayout mLayout;
    ArrayList<LabelFoundNode> foundLabeledNodes = new ArrayList<>();
    ArrayList<TooltipRequiredNode> tooltipRequiredNodes = new ArrayList<>();

    boolean isVoiceCommandConnected = false;

    int noOfLabels = 0;
    boolean hasExecuted = true;
    boolean skipPreviousRTECheck = false;
    SpeechRecognizer speechRecognizer;                      // declaring speech recognition var
    Intent speechRecognizerIntent;
    String debugLogTag = "FIT4003_VOICIFY";                  // use this tag for all log tags.
    ArrayList<String> launchTriggers = new ArrayList<String>(Arrays.asList("load", "launch", "execute", "open"));

    // Defining window manager for overlay elements and switchBar
    WindowManager wm;
    WindowManager.LayoutParams switchBar; // stores layout parameters for movable switchBar

    long currentTime;

    // variable for switch bar coordinates
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    ArrayList<String> savedCommands = new ArrayList<>();
    private int currentTooltipCount = 1;
    boolean isRecording = false;
    boolean isPlaying = false;

    // new code
    ArrayList<PackageDataObject> packageDataObjects = new ArrayList<>();
    int currentIntentIndex = 0;
    String currentOpeningPackage;
    String currentOpeningFeature;
    String currentAppName;
    String currentIntentIdentifier;
    ArrayList<String> matchedIntents = new ArrayList<>();
    ArrayList<String> matchedDeeplinks = new ArrayList<>();

    String[] tooltipColorSpinnerItems = new String[]{"#64b5f6", "#2b2b2b", "#ff4040"};
    int[] tooltipSizeSpinnerItems = new int[]{14, 18, 22};
    int[] tooltipOpacitySpinnerItems = new int[]{250, 220, 170, 120};
    int[] buttonOpacitySpinnerItems = new int[]{250, 220, 170, 120};
    boolean[] buttonRecordItems = new boolean[]{false, true};
    boolean[] buttonAlgoItems = new boolean[]{true, false};

    int tooltipColor = 0;
    int tooltipSize = 0;
    int tooltipOpacity = 0;
    int buttonOpacity = 0;
    int buttonRecordTxt = 0;
    int buttonAlgoTxt = 0;
    String previousEventCode = "";


    // Comment out
//    public Word2Vec model;

    // v11
    private long speechStartTime; // Global variable to track the start time
    //v11 end

    // v10 add new

// Interface Class Definitions and Structures - By Brennan

    // Hash maps for command handling - Brennan
    private HashMap<String, Object> commandMap = new HashMap<>();
    private HashMap<String, String> appMap = new HashMap<>();

    // CommandHandler interface
    public interface CommandHandler {
        void executeCommand(String[] tokens);
    }

    // OpenCommand Implementation - By Brennan
    public class OpenCommand implements CommandHandler {

        @Override
        public void executeCommand(String[] tokens) {
            if (tokens.length == 0) {
                Log.e("OpenCommand", "No app name provided.");
                return;
            }

            StringBuilder appNameBuilder = new StringBuilder();
            String packageName = null;
            int appNameEndIndex = -1;

            // Incrementally build the app name and check against appMap
            for (int i = 0; i < tokens.length; i++) {
                if (i > 0) {
                    appNameBuilder.append(" ");
                }
                appNameBuilder.append(tokens[i]);

                String potentialAppName = appNameBuilder.toString().toLowerCase();
                packageName = appMap.get(potentialAppName);
                if (packageName != null) {
                    appNameEndIndex = i; // Record the last token of the app name
                    break;
                }
            }

            if (packageName == null) {
                Log.e("OpenCommand", "App not mapped in appMap: " + appNameBuilder.toString());
                return;
            }

            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) {
                Log.e("OpenCommand", "App not found for: " + appNameBuilder.toString());
                return;
            }

            // Open the app
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.d("OpenCommand", "Opened app: " + appNameBuilder.toString());

            // Stop further processing if no additional tokens exist after the app name
            if (appNameEndIndex == tokens.length - 1) {
                Log.d("OpenCommand", "No additional tokens to process. Ending command.");
                return;
            }

// Handle remaining tokens
            // Define valid transition words
            List<String> transitionWords = Arrays.asList("and", "then", "so", "next", "after");

            // Define delay duration in milliseconds
            final long DELAY_BETWEEN_COMPLEX_COMMANDS_MS = 1650;

            String[] remainingTokens = Arrays.copyOfRange(tokens, appNameEndIndex + 1, tokens.length);

            if (remainingTokens.length > 0) {
                int startIndex = 0;

                // Skip all consecutive transition words
                while (startIndex < remainingTokens.length && transitionWords.contains(remainingTokens[startIndex].toLowerCase())) {
                    Log.d("OpenCommand", "Skipping transition word: '" + remainingTokens[startIndex] + "'");
                    startIndex++;
                }

                // Ensure there are tokens left after skipping transition words
                if (startIndex < remainingTokens.length) {
                    // Check if the next token is a top-level command
                    if (isTopLevelCommand(remainingTokens[startIndex])) {
                        Log.d("OpenCommand", "Detected new top-level command after transition words.");

                        // Create copies of effectively final variables for the lambda
                        final String[] finalRemainingTokens = Arrays.copyOfRange(remainingTokens, startIndex, remainingTokens.length);

                        // Introduce a small delay for complex commands
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Log.d("OpenCommand", "Executing delayed traversal for top-level command.");
                            traverseCommandMap(finalRemainingTokens, commandMap, 0);
                        }, DELAY_BETWEEN_COMPLEX_COMMANDS_MS);

                        return; // Stop further processing to allow the delayed execution
                    } else {
                        // Otherwise, process the remaining tokens as subcommands
                        Log.d("OpenCommand", "Passing remaining tokens to subcommands: " + Arrays.toString(Arrays.copyOfRange(remainingTokens, startIndex, remainingTokens.length)));
                        HashMap<String, Object> appSubcommands = getSubcommandsForApp(appNameBuilder.toString());
                        if (appSubcommands != null) {
                            traverseCommandMap(Arrays.copyOfRange(remainingTokens, startIndex, remainingTokens.length), appSubcommands, 0);
                        } else {
                            Log.e("OpenCommand", "No subcommands for app: " + appNameBuilder.toString());
                        }
                    }
                } else {
                    Log.d("OpenCommand", "No valid tokens to process after skipping transition words.");
                }
            } else {
                Log.d("OpenCommand", "No remaining tokens to process.");
            }



        }

        private boolean isTopLevelCommand(String token) {
            // A token is a top-level command if it exists in the root commandMap
            return commandMap.containsKey(token);
        }

        private HashMap<String, Object> getSubcommandsForApp(String appName) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> openCommandMap = (HashMap<String, Object>) commandMap.get("open");
            if (openCommandMap == null) {
                Log.e("OpenCommand", "'open' command not found in commandMap.");
                return null;
            }

            @SuppressWarnings("unchecked")
            HashMap<String, Object> subcommands = (HashMap<String, Object>) openCommandMap.get("subcommands");
            if (subcommands == null) {
                Log.e("OpenCommand", "No subcommands for 'open'.");
                return null;
            }

            @SuppressWarnings("unchecked")
            HashMap<String, Object> appCommandMap = (HashMap<String, Object>) subcommands.get(appName);
            if (appCommandMap == null) {
                Log.e("OpenCommand", "No mappings for app: " + appName);
                return null;
            }

            return (HashMap<String, Object>) appCommandMap.get("subcommands");
        }
    }


    //    // Play youtube video -- by Hei
//    // PlayCommand Implementation
    public class PlayCommand implements CommandHandler {
        private final String packageName;

        public PlayCommand(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public void executeCommand(String[] tokens) {
            if (tokens.length == 0) {
                Log.e("PlayCommand", "No video query provided.");
                return;
            }

            String query = String.join(" ", tokens);
            Log.d("PlayCommand", "Playing YouTube video with query: " + query);

            // Intent for opening YouTube
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // Format the URL for playing the video on YouTube
            String youtubeUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(query);

            // Set the intent data (the YouTube URL)
            intent.setData(Uri.parse(youtubeUrl));
            intent.setPackage(packageName);  // Specify the YouTube app
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                // Start activity to open YouTube and perform the search or video play
                startActivity(intent);
            } catch (Exception e) {
                Log.e("PlayCommand", "Failed to play video on YouTube.", e);
            }
        }
    }

    // SearchCommand Implementation - By Brennan
    public class SearchCommand implements CommandHandler {
        private final String packageName; // The package name for the app
        private final String actionType; // Custom behavior type (if needed)

        // Constructor to initialize the app's package name or behavior
        public SearchCommand(String packageName, String actionType) {
            this.packageName = packageName;
            this.actionType = actionType;
        }

        @Override
        public void executeCommand(String[] tokens) {
            if (tokens.length == 0) {
                Log.e("SearchCommand", "No search query provided.");
                return;
            }

            String query = String.join(" ", tokens);
            Log.d("SearchCommand", "Searching in " + packageName + " with query: " + query);

            // Intent for searching in different apps
            if ("youtube".equals(actionType)) {
                Intent intent = new Intent(Intent.ACTION_SEARCH);
                intent.setPackage(packageName); // YouTube's package
                intent.putExtra("query", query);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else if ("maps".equals(actionType)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setPackage(packageName); // Maps' package
                intent.setData(Uri.parse("geo:0,0?q=" + Uri.encode(query))); // Use geo URI for Maps
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Log.e("SearchCommand", "Unsupported action type: " + actionType);
            }
        }
    }

    // GoHomeCommand Implementation - Brennan
    public class GoHomeCommand implements CommandHandler {
        @Override
        public void executeCommand(String[] tokens) {
            Log.d("GoHomeCommand", "Going to the home screen.");
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    // GoBackCommand Implementation - Brennan
    public class GoBackCommand implements CommandHandler {
        @Override
        public void executeCommand(String[] tokens) {
            Log.d("GoBackCommand", "Going back.");
            // Simulate a back button press

            performGlobalAction(GLOBAL_ACTION_BACK);

            try {
                Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_BACK);
            } catch (IOException e) {
                Log.e("GoBackCommand", "Failed to execute back command.", e);
            }
        }
    }

    // ScrollSwipeCommand Implementation - By Brennan
    public class ScrollSwipeCommand implements CommandHandler {
        private final AccessibilityService accessibilityService;

        public ScrollSwipeCommand(AccessibilityService service) {
            this.accessibilityService = service; // Pass the AccessibilityService instance
        }

        @Override
        public void executeCommand(String[] tokens) {
            if (tokens.length == 0) {
                Log.e("ScrollSwipeCommand", "No direction specified.");
                return;
            }

            String direction = tokens[0].toLowerCase();
            Log.d("ScrollSwipeCommand", "Received scroll/swipe command: " + direction);

            switch (direction) {
                case "left":
                    performSwipeLeft();
                    break;
                case "right":
                    performSwipeRight();
                    break;
                case "up":
                    performSwipeUp();
                    break;
                case "down":
                    performSwipeDown();
                    break;
                default:
                    Log.e("ScrollSwipeCommand", "Unrecognized swipe direction: " + direction);
            }
        }

        private void performSwipeLeft() {
            performSwipe(0.8f, 0.2f, 0.5f, 0.5f); // Swipe from right to left
        }

        private void performSwipeRight() {
            performSwipe(0.2f, 0.8f, 0.5f, 0.5f); // Swipe from left to right
        }

        private void performSwipeUp() {
            performSwipe(0.5f, 0.5f, 0.8f, 0.2f); // Swipe from bottom to top
        }

        private void performSwipeDown() {
            performSwipe(0.5f, 0.5f, 0.2f, 0.8f); // Swipe from top to bottom
        }

        private void performSwipe(float startX, float endX, float startY, float endY) {
            int screenWidth = accessibilityService.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = accessibilityService.getResources().getDisplayMetrics().heightPixels;

            // Calculate absolute screen coordinates
            float startXCoord = startX * screenWidth;
            float endXCoord = endX * screenWidth;
            float startYCoord = startY * screenHeight;
            float endYCoord = endY * screenHeight;

            // Create a Path for the swipe action
            Path swipePath = new Path();
            swipePath.moveTo(startXCoord, startYCoord); // Start point
            swipePath.lineTo(endXCoord, endYCoord); // End point

            // Create a gesture builder for swipe action
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(swipePath, 0, 300); // Duration in milliseconds
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(stroke);

            // Dispatch the gesture
            accessibilityService.dispatchGesture(gestureBuilder.build(), null, null);
            Log.d("ScrollSwipeCommand", "Swipe performed: (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ")");
        }
    }




    // RouteCommand Implementation - By Brennan
    public class RouteCommand implements CommandHandler {

        @Override
        public void executeCommand(String[] tokens) {
            if (tokens.length == 0) {
                Log.e("RouteCommand", "No location provided.");
                return;
            }

            // Preprocess tokens to remove filler words like "to"
            String[] filteredTokens = preprocessRouteTokens(tokens);

            // Join the filtered tokens to form the location query
            String locationQuery = String.join(" ", filteredTokens);
            Log.d("RouteCommand", "Routing to location: " + locationQuery);

            // Create an Intent to start navigation
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage("com.google.android.apps.maps"); // Use Google Maps
            intent.setData(Uri.parse("google.navigation:q=" + Uri.encode(locationQuery))); // Encode the location query
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e("RouteCommand", "Failed to start navigation to: " + locationQuery, e);
            }
        }

        // Helper method to preprocess tokens for routing
        private String[] preprocessRouteTokens(String[] tokens) {
            List<String> filteredTokens = new ArrayList<>();
            for (String token : tokens) {
                // Skip filler words
                if (!token.equalsIgnoreCase("to") && !token.equalsIgnoreCase("nearest") && !token.equalsIgnoreCase("near me") && !token.equalsIgnoreCase("a")) {
                    filteredTokens.add(token);
                }
            }
            return filteredTokens.toArray(new String[0]);
        }
    }

    // TakePictureCommand Implementation - Brennan
    public class TakePictureCommand implements CommandHandler {
        @Override
        public void executeCommand(String[] tokens) {
            Log.d("TakePictureCommand", "Opening camera app and attempting to click the 'Take picture' button.");

            // Get the camera app package name
            String cameraPackage = appMap.get("camera");
            if (cameraPackage == null) {
                Log.e("TakePictureCommand", "Camera app not mapped in appMap.");
                return;
            }

            // Use the OpenCommand logic to open the camera app
            Intent intent = getPackageManager().getLaunchIntentForPackage(cameraPackage);
            if (intent == null) {
                Log.e("TakePictureCommand", "Camera app not found for package: " + cameraPackage);
                return;
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
                Log.d("TakePictureCommand", "Camera app opened.");

                // Wait for a short delay to ensure the app is fully opened
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Use accessibility service to find and click the button
                    clickTakePictureButton();
                }, 2000); // Adjust delay based on device/app load time

            } catch (Exception e) {
                Log.e("TakePictureCommand", "Failed to open camera app.", e);
            }
        }

        private void clickTakePictureButton() {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Log.e("TakePictureCommand", "Root node is null. Cannot find 'Take picture' button.");
                return;
            }

            // Find the button using its contentDescription
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText("Take picture");
            for (AccessibilityNodeInfo node : nodes) {
                if (node.isClickable() && "Take picture".equals(node.getContentDescription())) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d("TakePictureCommand", "Successfully clicked the 'Take picture' button.");
                    return;
                }
            }

            Log.e("TakePictureCommand", "Failed to find or click the 'Take picture' button.");
        }
    }

    // ToggleCameraCommand Implementation - Brennan
    public class ToggleCameraCommand implements CommandHandler {

        @Override
        public void executeCommand(String[] tokens) {
            Log.d("ToggleCameraCommand", "Toggling between front and rear cameras.");

            // Get the root node of the current screen
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Log.e("ToggleCameraCommand", "Root node is null. Cannot toggle camera.");
                return;
            }

            // Search for both "Switch to front camera" and "Switch to rear camera" buttons
            List<AccessibilityNodeInfo> frontCameraNodes = rootNode.findAccessibilityNodeInfosByText("Switch to front camera");
            List<AccessibilityNodeInfo> rearCameraNodes = rootNode.findAccessibilityNodeInfosByText("Switch to rear camera");

            try {
                // If "Switch to front camera" is visible and clickable, click it
                for (AccessibilityNodeInfo node : frontCameraNodes) {
                    if (node.isClickable() && node.isVisibleToUser()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d("ToggleCameraCommand", "Switched to front camera.");
                        return;
                    }
                }

                // If "Switch to rear camera" is visible and clickable, click it
                for (AccessibilityNodeInfo node : rearCameraNodes) {
                    if (node.isClickable() && node.isVisibleToUser()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d("ToggleCameraCommand", "Switched to rear camera.");
                        return;
                    }
                }

                // If neither button is found, log an error
                Log.e("ToggleCameraCommand", "No camera switch button found.");
            } catch (Exception e) {
                Log.e("ToggleCameraCommand", "Failed to toggle camera.", e);
            }
        }
    }

    // TakeSelfieCommand Implementation - Brennan
    public class TakeSelfieCommand implements CommandHandler {

        @Override
        public void executeCommand(String[] tokens) {
            Log.d("TakeSelfieCommand", "Executing TakeSelfieCommand: Ensuring front camera is active, then taking a picture.");

            new Handler(Looper.getMainLooper()).post(() -> {
                // Step 1: Open the camera
                CommandHandler openCamera = new OpenCommand();
                openCamera.executeCommand(new String[]{"camera"});

                // Step 2: Add a delay to ensure the camera app has opened
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Step 3: Check camera state and switch if needed
                    ensureFrontCamera(() -> {
                        // Step 4: Add a delay after switching to the front camera
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // Step 5: Take a picture
                            Log.d("TakeSelfieCommand", "Taking picture with front camera.");
                            CommandHandler takePicture = new TakePictureCommand();
                            takePicture.executeCommand(new String[]{});
                        }, 500); // Shorter delay for better responsiveness
                    });
                }, 1000); // Delay for opening the camera
            });
        }

        private void ensureFrontCamera(Runnable onComplete) {
            Log.d("TakeSelfieCommand", "Checking if front camera is active.");

            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                Log.e("TakeSelfieCommand", "Root node is null. Cannot check camera state.");
                if (onComplete != null) onComplete.run();
                return;
            }

            // Find the "Switch to front camera" button to check if the rear camera is active
            List<AccessibilityNodeInfo> frontCameraNodes = rootNode.findAccessibilityNodeInfosByText("Switch to front camera");

            boolean isRearCameraActive = false;
            for (AccessibilityNodeInfo node : frontCameraNodes) {
                if (node.isVisibleToUser() && node.isClickable()) {
                    isRearCameraActive = true; // Rear camera is active if the button to switch to the front camera is visible
                    break;
                }
            }

            if (isRearCameraActive) {
                Log.d("TakeSelfieCommand", "Rear camera is active. Switching to front camera.");
                CommandHandler toggleCamera = new ToggleCameraCommand();
                toggleCamera.executeCommand(new String[]{});

                // Add a delay before calling the completion callback
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (onComplete != null) onComplete.run();
                }, 1500); // Allow time for the camera to switch
            } else {
                Log.d("TakeSelfieCommand", "Front camera is already active. Skipping camera flip.");
                if (onComplete != null) onComplete.run();
            }
        }
    }

    // ViewVideoSearchCommand Implementation - Brennan
    public class ViewVideoSearchCommand implements CommandHandler {
        @Override
        public void executeCommand(String[] tokens) {
            Log.d("ViewVideoSearchCommand", "Attempting to open the 'Videos' page using intent.");

            try {
                // Create an intent with the provided action and component
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setComponent(new ComponentName(
                        "com.cookware.worldcusinerecipes",
                        "com.cookware.worldcusinerecipes.VideosActivity" // Target activity
                ));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensure it opens as a new task

                // Start the target activity
                startActivity(intent);
                Log.d("ViewVideoSearchCommand", "Successfully navigated to the 'Videos' page using intent.");
            } catch (Exception e) {
                Log.e("ViewVideoSearchCommand", "Failed to navigate to the 'Videos' page using intent.", e);
            }
        }

    }

    // ViewVideoSearchCommand Implementation - Brennan
    public class ViewRecipeFavorites implements CommandHandler {
        @Override
        public void executeCommand(String[] tokens) {
            Log.d("ViewRecipeFavorites", "Attempting to open the 'Favorite Recipes' page using intent.");

            try {
                // Create an intent with the provided action and component
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setComponent(new ComponentName(
                        "com.cookware.worldcusinerecipes",
                        "com.cookware.worldcusinerecipes.FavoritesActivity" // Target activity
                ));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensure it opens as a new task

                // Start the target activity
                startActivity(intent);
                Log.d("ViewRecipeFavorites", "Successfully navigated to the 'Favorites' page using intent.");
            } catch (Exception e) {
                Log.e("ViewRecipeFavorites", "Failed to navigate to the 'Favorites' page using intent.", e);
            }
        }
    }

    // ViewVideosBookmarked Implementation - Brennan
    public class ViewVideosBookmarked implements CommandHandler {
        @Override
        public void executeCommand(String[] tokens) {
            Log.d("ViewVideosBookmarked", "Attempting to open the 'Videos Bookmarked' page using intent.");

            try {
                // Create an intent with the provided action and component
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setComponent(new ComponentName(
                        "com.cookware.worldcusinerecipes",
                        "com.cookware.worldcusinerecipes.VideosBookmarkedActivity" // Target activity
                ));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensure it opens as a new task

                // Start the target activity
                startActivity(intent);
                Log.d("ViewVideosBookmarked", "Successfully navigated to the 'Videos Bookmarked' page using intent.");
            } catch (Exception e) {
                Log.e("ViewVideosBookmarked", "Failed to navigate to the 'Videos Bookmarked' page using intent.", e);
            }
        }
    }

    // ViewVideosBookmarked Implementation - Brennan
    public class ViewUserProfile implements CommandHandler {
        @Override
        public void executeCommand(String[] tokens) {
            Log.d("ViewUserProfile", "Attempting to open the 'User Profile' page using intent.");

            try {
                // Create an intent with the provided action and component
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setComponent(new ComponentName(
                        "com.cookware.worldcusinerecipes",
                        "com.cookware.worldcusinerecipes.UserProfileActivity" // Target activity
                ));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensure it opens as a new task

                // Start the target activity
                startActivity(intent);
                Log.d("ViewUserProfile", "Successfully navigated to the 'User Profile' page using intent.");
            } catch (Exception e) {
                Log.e("ViewUserProfile", "Failed to navigate to the 'User Profile' page using intent.", e);
            }
        }
    }

    // ViewRecipeStore Implementation - Brennan
    public class ViewRecipeStore implements CommandHandler {
        @Override
        public void executeCommand(String[] tokens) {
            Log.d("ViewRecipeStore", "Attempting to open the 'Recipe Store' page using intent.");

            try {
                // Create an intent with the provided action and component
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setComponent(new ComponentName(
                        "com.cookware.worldcusinerecipes",
                        "com.cookware.worldcusinerecipes.RecipeStoreActivity" // Target activity
                ));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensure it opens as a new task

                // Start the target activity
                startActivity(intent);
                Log.d("ViewRecipeStore", "Successfully navigated to the 'Recipe Store' page using intent.");
            } catch (Exception e) {
                Log.e("ViewRecipeStore", "Failed to navigate to the 'Recipe Store' page using intent.", e);
            }
        }
    }

/* setupCommandMappings() - Brennan
commandMap
├── "open" → openCommandMap
│   ├── "action" → OpenCommand
│   └── "subcommands" → openSubcommands
│       ├── "youtube" → youtubeCommandMap
│       │   ├── "action" → null (no direct action for "youtube")
│       │   └── "subcommands" → youtubeSubcommands
│       │       ├── "search" → youtubeSearchCommandMap
│       │       │   └── "action" → SearchCommand("com.google.android.youtube", "youtube")
│       │       └── "play" → youtubePlayCommandMap
│       │           └── "action" → PlayCommand("com.google.android.youtube")
│       ├── "maps" → mapsCommandMap
│       │   ├── "action" → null (no direct action for "maps")
│       │   └── "subcommands" → mapsSubcommands
│       │       ├── "search" → mapsSearchCommandMap
│       │       │   └── "action" → SearchCommand("com.google.android.apps.maps", "maps")
│       │       └── "route" → mapsRouteCommandMap
│       │           └── "action" → RouteCommand
│       └── "camera" → cameraCommandMap
│           ├── "action" → null (no direct action for "camera")
│           └── "subcommands" → cameraSubcommands
│               ├── "take" → takePictureCommandMap
│               │   └── "action" → TakePictureCommand
│               └── "flip" → toggleCameraCommandMap
│                   └── "action" → ToggleCameraCommand
├── "home" → goHomeCommandMap
│   └── "action" → GoHomeCommand
├── "back" → goBackCommandMap
│   └── "action" → GoBackCommand
├── "scroll" → scrollCommandMap
│   └── "action" → ScrollCommand
├── "photo" → takePictureCommandMap
│   └── "action" → TakePictureCommand
├── "picture" → takePictureCommandMap
│   └── "action" → TakePictureCommand
├── "flip" → toggleCameraCommandMap
│   └── "action" → ToggleCameraCommand
├── "camera" → toggleCameraCommandMap
│   └── "action" → ToggleCameraCommand
└── "route" → routeCommandMap
    └── "action" → RouteCommand

 */

    // Setup Command Mappings - By Brennan
    private void setupCommandMappings() {
        // === YouTube Commands ===
        HashMap<String, Object> youtubeSearchCommandMap = new HashMap<>();
        youtubeSearchCommandMap.put("action", new SearchCommand("com.google.android.youtube", "youtube"));

        HashMap<String, Object> youtubePlayCommandMap = new HashMap<>();
        youtubePlayCommandMap.put("action", new PlayCommand("com.google.android.youtube"));

        HashMap<String, Object> youtubeCommandMap = new HashMap<>();
        youtubeCommandMap.put("action", null);
        youtubeCommandMap.put("subcommands", new HashMap<String, Object>() {{
            put("search", youtubeSearchCommandMap);
            put("play", youtubePlayCommandMap);
        }});

        commandMap.put("youtube", youtubeCommandMap);

        // === Maps Commands ===
        HashMap<String, Object> mapsSearchCommandMap = new HashMap<>();
        mapsSearchCommandMap.put("action", new SearchCommand("com.google.android.apps.maps", "maps"));

        HashMap<String, Object> mapsRouteCommandMap = new HashMap<>();
        mapsRouteCommandMap.put("action", new RouteCommand());

        HashMap<String, Object> mapsCommandMap = new HashMap<>();
        mapsCommandMap.put("action", null);
        mapsCommandMap.put("subcommands", new HashMap<String, Object>() {{
            put("search", mapsSearchCommandMap);
            put("route", mapsRouteCommandMap);
        }});

        commandMap.put("maps", mapsCommandMap);

        // === Camera Commands ===
        HashMap<String, Object> takePictureCommandMap = new HashMap<>();
        takePictureCommandMap.put("action", new TakePictureCommand());

        HashMap<String, Object> toggleCameraCommandMap = new HashMap<>();
        toggleCameraCommandMap.put("action", new ToggleCameraCommand());

        HashMap<String, Object> takeSelfieCommandMap = new HashMap<>();
        takeSelfieCommandMap.put("action", new TakeSelfieCommand());

        HashMap<String, Object> cameraCommandMap = new HashMap<>();
        cameraCommandMap.put("action", null);
        cameraCommandMap.put("subcommands", new HashMap<String, Object>() {{
            put("take", takePictureCommandMap);
            put("flip", toggleCameraCommandMap);
            put("selfie", takeSelfieCommandMap);
        }});

        commandMap.put("camera", cameraCommandMap);
        commandMap.put("photo", takePictureCommandMap);
        commandMap.put("picture", takePictureCommandMap);
        commandMap.put("flip", toggleCameraCommandMap);
        commandMap.put("selfie", takeSelfieCommandMap);

        // === World Cuisines Commands ===
        HashMap<String, Object> viewVideoSearchCommandMap = new HashMap<>();
        viewVideoSearchCommandMap.put("action", new ViewVideoSearchCommand());

        HashMap<String, Object> viewRecipeFavoritesCommandMap = new HashMap<>();
        viewRecipeFavoritesCommandMap.put("action", new ViewRecipeFavorites());

        HashMap<String, Object> viewVideosBookmarkedCommandMap = new HashMap<>();
        viewVideosBookmarkedCommandMap.put("action", new ViewVideosBookmarked());

        HashMap<String, Object> viewUserProfileCommandMap = new HashMap<>();
        viewUserProfileCommandMap.put("action", new ViewUserProfile());

        HashMap<String, Object> viewRecipeStoreCommandMap = new HashMap<>();
        viewRecipeStoreCommandMap.put("action", new ViewRecipeStore());

        HashMap<String, Object> worldCuisinesCommandMap = new HashMap<>();
        worldCuisinesCommandMap.put("action", null);
        worldCuisinesCommandMap.put("subcommands", new HashMap<String, Object>() {{
            put("go to view video search", viewVideoSearchCommandMap);
            put("show me view favorite detail", viewRecipeFavoritesCommandMap);
            put("show me view videos bookmarked", viewVideosBookmarkedCommandMap);
            put("show me view user profile", viewUserProfileCommandMap);
            put("show me view recipe store", viewRecipeStoreCommandMap);
        }});

        // === Open Commands ===
        HashMap<String, Object> openCommandMap = new HashMap<>();
        openCommandMap.put("action", new OpenCommand());
        openCommandMap.put("subcommands", new HashMap<String, Object>() {{
            put("youtube", youtubeCommandMap);
            put("maps", mapsCommandMap);
            put("camera", cameraCommandMap);
            put("world cuisines", worldCuisinesCommandMap);
        }});

        commandMap.put("open", openCommandMap);

        // === Navigation Commands ===
        HashMap<String, Object> goHomeCommandMap = new HashMap<>();
        goHomeCommandMap.put("action", new GoHomeCommand());
        commandMap.put("home", goHomeCommandMap);

        HashMap<String, Object> goBackCommandMap = new HashMap<>();
        goBackCommandMap.put("action", new GoBackCommand());
        commandMap.put("back", goBackCommandMap);

        // === Scroll Commands ===
        // Combined Scroll/Swipe Command
        ScrollSwipeCommand scrollSwipeCommand = new ScrollSwipeCommand(this); // Pass 'this' as AccessibilityService

        // Add scroll and swipe commands to commandMap
        commandMap.put("scroll", new HashMap<String, Object>() {{
            put("action", scrollSwipeCommand);
        }});

        commandMap.put("swipe", new HashMap<String, Object>() {{
            put("action", scrollSwipeCommand);
        }});


        // === Route Commands ===
        HashMap<String, Object> routeCommandMap = new HashMap<>();
        routeCommandMap.put("action", new RouteCommand());
        commandMap.put("route", routeCommandMap);

        // Log the command map structure
        Log.d("setupCommandMappings", "Command map structure: " + new Gson().toJson(commandMap));
    }

    // Traverse Command Map - By Brennan
    private void traverseCommandMap(String[] tokens, HashMap<String, Object> currentMap, int index) {
        int topLevelCommandCount = 0; // Counter for top-level commands

        while (index < tokens.length) {
            StringBuilder phraseBuilder = new StringBuilder();
            int phraseEndIndex = -1;

            // Attempt to match multi-word keys by building phrases from remaining tokens
            for (int i = index; i < tokens.length; i++) {
                if (i > index) phraseBuilder.append(" "); // Add a space between words
                phraseBuilder.append(tokens[i]);
                String phrase = phraseBuilder.toString();

                if (currentMap.containsKey(phrase)) {
                    phraseEndIndex = i;
                    break; // Stop searching once the longest matching phrase is found
                }
            }

            if (phraseEndIndex != -1) {
                // Found a matching phrase
                String matchingPhrase = phraseBuilder.toString();
                @SuppressWarnings("unchecked")
                HashMap<String, Object> entry = (HashMap<String, Object>) currentMap.get(matchingPhrase);

                // Check if the matching phrase is a top-level command
                if (currentMap == commandMap) {
                    topLevelCommandCount++;
                    if (topLevelCommandCount > 1) {
                        Log.d("CommandExecution", "Multiple top-level commands detected. Restarting for new command.");
                        traverseCommandMap(tokens, commandMap, phraseEndIndex); // Restart for the new top-level command
                        return; // End the current traversal
                    }
                }

                // Execute associated action if it exists
                CommandHandler action = (CommandHandler) entry.get("action");
                if (action != null) {
                    String[] remainingTokens = Arrays.copyOfRange(tokens, phraseEndIndex + 1, tokens.length);
                    Log.d("CommandExecution", "Executing action for phrase: " + matchingPhrase);
                    action.executeCommand(remainingTokens);
                    return; // Stop further processing after executing the action
                }

                // Traverse subcommands if they exist
                @SuppressWarnings("unchecked")
                HashMap<String, Object> subcommands = (HashMap<String, Object>) entry.get("subcommands");
                if (subcommands != null) {
                    Log.d("CommandExecution", "Traversing subcommands for phrase: " + matchingPhrase);
                    index = phraseEndIndex + 1; // Move to the next token
                    currentMap = subcommands; // Switch context to subcommands
                    continue; // Restart traversal in the subcommand context
                } else {
                    Log.e("CommandExecution", "No subcommands for phrase: " + matchingPhrase);
                }
            }

            // If no matching phrase is found, skip the current token
            Log.w("CommandExecution", "Skipping unrecognized token: " + tokens[index]);
            index++;
        }

        Log.d("CommandExecution", "All tokens processed.");
    }

    // Execute Command - By Brennan
    private void executeCommand(String sentence) {
        long startedTime = System.currentTimeMillis(); // Start timing

        // Split the input sentence into lowercase tokens for consistent processing
        String[] tokens = sentence.toLowerCase().trim().split(" ");
        Log.d("CommandExecution", "Executing command: " + sentence);

        // Start traversing the command map from the root.
        traverseCommandMap(tokens, commandMap, 0);

        long endedTime = System.currentTimeMillis(); // End timing

        // Uncomment if test execution time using testcommand variable or using simulated voice commands
//        Log.d("CommandExecution", "Parser Execution Time (using test command/simulate voice)'" + "': " + (endedTime - startedTime) + " ms");

    }


    // v10 end

    // ************ comment out old onAccessibilityEvent()
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        /**
         * This function will be invoked when defined type of event occurs
         * param: event is an instance that capture every information about the event
         */

        // basic checks for null safety
        AccessibilityNodeInfo source = event.getSource();
        Log.d("dddd", event.getEventType() + "  " + event.getSource());
//        if (source == null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
//            return;
//        }
//
//        if (previousSource != null && source != null && (previousSource.hashCode() == source.hashCode() || previousSource.equals(source))) {
//            Log.d(debugLogTag, "Blocked event");
//
//            return;
//        }
//
//        if (previousEventCode.equals("32") && previousSource != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
//            Log.d(debugLogTag, "Overlapped event");
//            return;
//        }

        if (source == null || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        // Prevent repeated events
        if (previousSource != null && source != null &&
                (previousSource.hashCode() == source.hashCode() || previousSource.equals(source))) {
            Log.d(debugLogTag, "Blocked duplicate event");
            return;
        }

        if (previousEventCode.equals("32") && previousSource != null &&
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d(debugLogTag, "Overlapped event blocked");
            return;
        }

        previousSource = source;
        previousEventCode = event.getEventType() + "";

        if (isNotBlockedEvent()) {
            checkSettingsChanged();
            uiElements.clear();
            noOfLabels = 0;
            removeAllTooltips();    // remove all old  tooltip when screen changed
            currentSource = getRootInActiveWindow(); // update the current root node

//            if (isVoiceCommandConnected && currentSource != null) {
//                printOutAllClickableElement(getRootInActiveWindow(), 0, event); // call function for root node
//                uiElements.remove("blocked numbers storage");
//            }
            Log.d("test", uiElements.toString());
            this.executeCommand("");
        }

        // add enw
        // Process the current UI hierarchy
//        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
//        if (rootNode != null) {
//            Log.d("AccessibilityEvent", "Root node is available. Printing clickable nodes...");
//
//            // Traverse and log all clickable components
//            logClickableComponents(rootNode);
//        } else {
//            Log.e("AccessibilityEvent", "Root node is null. Cannot traverse.");
//        }
        //end
    }
    // ****** end

    // add new
    private void logClickableComponents(AccessibilityNodeInfo node) {
        if (node == null) return;

        // Check if the node is clickable and visible
        if (node.isClickable() && node.isVisibleToUser()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            Log.d("ClickableNode",
                    "Text: " + node.getText() +
                            ", Content Description: " + node.getContentDescription() +
                            ", View ID: " + node.getViewIdResourceName() +
                            ", Bounds: " + bounds +
                            ", Class Name: " + node.getClassName());
        }

        // Recursively log children nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            logClickableComponents(node.getChild(i));
        }
    }
    // end

    public void checkSettingsChanged() {
        sharedPreferences = getSharedPreferences(FILE_NAME, 0);
        tooltipColor = sharedPreferences.getInt(SettingsActivity.TOOLTIP_COLOR, 0);
        tooltipSize = sharedPreferences.getInt(SettingsActivity.TOOLTIP_SIZE, 0);
        tooltipOpacity = sharedPreferences.getInt(SettingsActivity.TOOLTIP_OPACITY, 0);
        int previousBtnOpacity = buttonOpacity;
        int previousBtnRecord = buttonRecordTxt;
        int previousBtnAlgo = buttonAlgoTxt;
        buttonOpacity = sharedPreferences.getInt(SettingsActivity.BUTTON_OPACITY, 0);
        buttonRecordTxt = sharedPreferences.getInt(SettingsActivity.BUTTON_RECORD, 0);
        buttonAlgoTxt = sharedPreferences.getInt(SettingsActivity.BUTTON_ALGO, 0);
        if (previousBtnOpacity != buttonOpacity || buttonRecordTxt != previousBtnRecord) {
            wm.removeView(mLayout);
            createSwitch();
        }
    }

    final Handler autoReloadHandler = new Handler();
    final Runnable runnable = new Runnable() {
        public void run() {
            autoReload();
            autoReloadHandler.postDelayed(this, 3000);

        }
    };


    public boolean isNotBlockedEvent() {
        Date date = new Date();
        long time = date.getTime();
        if (time - currentTime > 750) {
            currentTime = time;
            return true;
        }
        return false;
    }

    public void autoReload() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager.isEnabled()) {

            AccessibilityEvent e = AccessibilityEvent.obtain();
            e.setEventType(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
            e.setClassName(getClass().getName());
            //Log.d(debugLogTag, "Auto Reloaded");
        }
    }

    public void printOutAllClickableElement(AccessibilityNodeInfo nodeInfo, int depth, AccessibilityEvent event) {
        /**
         * This function will print out all clickable element, storing the data it has or number for those
         * clickable elements.
         *
         */


        if (nodeInfo == null) {
            return;
        }
        if (nodeInfo.isClickable()) {
            String label = "";
            Rect rectTest = new Rect();                     //  to get the coordinate of the UI element
            nodeInfo.getBoundsInScreen(rectTest);           //  store data of the node
            if (rectTest.right - 100 < width && rectTest.bottom - 100 < height && rectTest.left + 100 > 0 && rectTest.top + 100 > 0) {
                if (nodeInfo.getText() != null) {   // check if node has a corresponding text
                    label += nodeInfo.getText();
                    Log.d(debugLogTag, "Clickable Element: " + label);
                    uiElements.add(label.toLowerCase());
                    return;
                } else {
                    // no information about node or event (Tags to be assigned!)
                    String foundLabel = searchForTextView(nodeInfo, "");
                    Log.d(debugLogTag, "Clickable Element with no text, found label: " + foundLabel);

                    String[] texts = foundLabel.split(" ");
                    int end = texts.length;
                    Set<String> cleanTexts = new HashSet<String>();

                    for (int i = 0; i < end; i++) {
                        cleanTexts.add(texts[i]);
                    }
                    String finalText = "";
                    for (String cleanText : cleanTexts) {
                        finalText += cleanText + " ";
                    }

                    if (!foundLabel.equals("") && noOfLabels < 15 && cleanTexts.size() < 10) {
                        foundLabeledNodes.add(new LabelFoundNode(nodeInfo, foundLabel.toLowerCase()));
                        uiElements.add(foundLabel.toLowerCase());

                        noOfLabels += 1;
                    } else if (currentTooltipCount < 20) {
                        inflateTooltip((rectTest.right + rectTest.left + 15) / 2, (rectTest.bottom + rectTest.top - 70) / 2, nodeInfo);    // call function to create number tooltips
                    }
                }
            }

            //clickableNodes.add(new Pair<>(label,nodeInfo));
            //Log.d(debugLogTag,"Available commands: " + label);
        }
        for (int i = 0; i < nodeInfo.getChildCount(); ++i) {
            printOutAllClickableElement(nodeInfo.getChild(i), depth + 1, event);    // recursive call
        }
    }

    public String filterDuplicateWord(String inputString) {
        String[] allWords;
        String result = "";

        // Split the given sentence to get each word as String array
        allWords = inputString.split(" ");
        // Use for loop to remove duplicate words
        for (int i = 0; i < allWords.length; i++) {
            for (int j = i + 1; j < allWords.length; j++) {
                if (allWords[i].equals(allWords[j])) {
                    allWords[j] = "$##@$@#";
                }
            }
        }
        // Convert to String
        for (String word : allWords) {
            if (!word.equals("$##@$@#")) {
                result = result + word + " ";
            }
        }

        return result;
    }


    private void inflateTooltip(int x, int y, AccessibilityNodeInfo nodeInfo) {
        /**
         * This function will configure each of the tooltip on the screen, so this function will be
         * called for each of the tooltip on the screen.
         * param: x is the location in x axis
         * param: y is the location in y axis
         */
        FrameLayout tooltipLayout = new FrameLayout(this);      // create new layout for each tooltip
        WindowManager.LayoutParams tooltipLayoutParams = new WindowManager.LayoutParams();
        tooltipLayoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        tooltipLayoutParams.format = PixelFormat.TRANSLUCENT;
        tooltipLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        tooltipLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        tooltipLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        tooltipLayoutParams.gravity = Gravity.TOP | Gravity.START;     // reset the (0,0) to the top left screen
        tooltipLayoutParams.x = x + 15;       // x location
        tooltipLayoutParams.y = y + 40;       // y location
        LayoutInflater inflater = LayoutInflater.from(this);
        inflater.inflate(R.layout.tooltip_number, tooltipLayout);   // inflate the view to the screen
        wm.addView(tooltipLayout, tooltipLayoutParams);

        TextView tooltip = tooltipLayout.findViewById(R.id.tooltip);    // set the count based on current count
        tooltip.setText(currentTooltipCount + "");
        tooltip.setTextSize(tooltipSizeSpinnerItems[tooltipSize]);
        tooltip.setBackgroundResource(R.drawable.tooltip_shape);  //drawable id
        GradientDrawable gd = (GradientDrawable) tooltip.getBackground().getCurrent();
        gd.setColor(Color.parseColor(tooltipColorSpinnerItems[tooltipColor])); //set color
        gd.setAlpha(tooltipOpacitySpinnerItems[tooltipOpacity]);        // add to the list to retrieve later
        gd.setSize(tooltipSizeSpinnerItems[tooltipSize] + 40, tooltipSizeSpinnerItems[tooltipSize] + 5);
        tooltipRequiredNodes.add(new TooltipRequiredNode(nodeInfo, currentTooltipCount, tooltipLayout));
        //change
        //uiElements.add(Integer.toString(currentTooltipCount));
        currentTooltipCount += 1;

    }


    private void initializeSpeechRecognition() {
        /**
         * This function performs all the steps required for speech recognition initialisation
         */

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // also available: LANGUAGE_MODEL_WEB_SEARCH
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // setting the limit for the service to listen as an requirement for API30
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 100000);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                //    Log.d("ss", "onReady");
                // textMsg.setText("Ready...");
                // textMsg.setBackgroundResource(R.color.green);
                // Called when the endpointer is ready for the user to start speaking.
            }

            @Override
            public void onBeginningOfSpeech() {
                //    Log.d("ss", "onBeginning");
                // The user has started to speak.
                // textMsg.setText("Listening...");
                // textMsg.setBackgroundResource(R.color.green);
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // The sound level in the audio stream has changed.
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                //Log.d("ss", "buffer");
                // More sound has been received.
            }

            @Override
            public void onEndOfSpeech() {
                // Log.d("ss", "onEndOfSpeech");
                // Called after the user stops speaking
            }

            @Override
            public void onError(int error) {
                //Log.d("ss", "onError: " + error);
                Log.d("hihi", error + " error");
                //Toast.makeText(MainActivity.this, "An error has occurred. Code: " + Integer.toString(error), Toast.LENGTH_SHORT).show();
                if (error == 8 || error == 7) {
                    speechRecognizer.cancel();
                    if (isVoiceCommandConnected) {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }
                }
            }

            @Override
            public void onResults(Bundle results) {
                // Called when recognition results are ready.

                //   textMsg.setText("Processing.");
                //   textMsg.setBackgroundResource(R.color.yellow);
                if (!isVoiceCommandConnected) {
                    return;
                }
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && matches.size() > 0) {
                    String match = matches.get(0);
                    //                  inputTxt.setText(match);
                    Log.d(debugLogTag, match);

                        long startedTime = System.currentTimeMillis(); // Start timing
                        executeCommand(match);
                        long endedTime = System.currentTimeMillis(); // End timing
//                        Log.d("CommandExecution", "End-to-End Time using voice: " + (endedTime - startedTime) + " ms");


                }

                speechRecognizer.startListening(speechRecognizerIntent);
            }


            @Override
            public void onPartialResults(Bundle partialResults) {
                //    textMsg.setText("Processing.");
                //    textMsg.setBackgroundResource(R.color.yellow);

                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && matches.size() > 0) {
                    String match = matches.get(0);
//                    inputTxt.setText(match);
                    Log.d(debugLogTag, match);

                    long commandStartTime = System.currentTimeMillis();
                    executeCommand(match);
                    long commandEndTime = System.currentTimeMillis();
                    Log.d("CommandExecution", "Parser Execution Time: " + (commandEndTime - commandStartTime) + " ms");
//                    Log.d("EndToEndTiming", "End-to-End Time: " + (commandEndTime - speechStartTime) + " ms");

                    // Reset speechStartTime for next command
                    speechStartTime = System.currentTimeMillis();

                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // reserved by android for future events
            }

        });
    }

    private String currentNumberOutput(int currentNumberTooltip) {
        String output = "";
        for (int i = 1; i <= currentNumberTooltip; i++) {
            output += i + ", ";
        }
        return output;
    }

    private String formatNumberInCommand(String sentence) {
        String[] numbers = {" zero", " one", " two", " three", " four", " five", " six", " seven", " eight", " nine", " ten", " eleven", " twelve", " thirteen", " fourteen", " fifteen", " sixteen", " seventeen", " eighteen", " nineteen"};
        sentence = sentence.replaceAll(" number", "").replaceAll("click to", "click two").replaceAll("press to", "click two").replaceAll("for", "four").replaceAll("want", "one").replaceAll("sex", "six");
        sentence = sentence.replaceAll("pressed", "press").replaceAll("clique", "click").replaceAll("quick", "click").replaceAll("Preston", "press");
        for (int i = 0; i < numbers.length; i++) {
            if (sentence.contains(numbers[i])) {
                sentence = sentence.replaceAll(numbers[i], " " + String.valueOf(i));
            }
        }
        return sentence;
    }

    // v12 (testing using simulated voice)
    private void simulateVoiceCommand(int audioResId, String simulatedText) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, audioResId);

        // Record the start time when audio begins
        long startTime = System.currentTimeMillis();

        // Get the audio duration
        int audioDuration = mediaPlayer.getDuration();

        // Start playing the audio
        mediaPlayer.start();

        // Simulate the processing by delaying the results
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Simulate onResults callback with the transcribed text
            onResultsSimulated(simulatedText, startTime);
            mediaPlayer.release();
        }, audioDuration);
    }

    private void onResultsSimulated(String simulatedText, long startTime) {
        ArrayList<String> matches = new ArrayList<>();
        matches.add(simulatedText);

        // Simulating the onResults call
        Log.d("SimulatedRecognizer", "Simulated recognized text: " + simulatedText);

        // Pass the simulated text to your executeCommand function
        executeCommand(simulatedText);

        // Measure the total end-to-end time
        long endTime = System.currentTimeMillis();
        Log.d("CommandExecution", "End-to-End Time using simulated voice: " + (endTime - startTime) + " ms");
    }
    // v12 updated end


    // ***********commented out
//    private void executeCommand(String sentence) {
//        sentence = formatNumberInCommand(sentence);
//        sentence = sentence.trim();
//
//
//        String currentCommand = "";
//        if (!hasExecuted) {
//            return;
//        }
//
//
//        if (sentence.equals("") && currentSequence.size() == 0) {
//            skipPreviousRTECheck = true;
//            return;
//        }
//
//        if (!sentence.equals("")) {
//            skipPreviousRTECheck = true;
//            String[] commands = sentence.split("and");
//            ArrayList<String> outputModelCommand = new ArrayList<String>();
//            Collections.addAll(currentSequence, commands);
//        }
//
//        if (!skipPreviousRTECheck) {
//            if (uiElements.equals(previousUIElements)) {
//                Log.d(debugLogTag, "No change in RTE: " + uiElements.toString() + ", " + previousUIElements.toString());
//                return;
//            }
//        }
//
//        String[] words = sentence.split(" ");
//        previousUIElements.clear();
//        previousUIElements.addAll(uiElements);
//        skipPreviousRTECheck = false;
//        boolean isExecuted = false;
//
//        if (currentSequence.size() != 0) {
//            //extract command
//            currentCommand = currentSequence.remove(0);
//
//            if (currentCommand.contains("back")) {
//                performGlobalAction(GLOBAL_ACTION_BACK);
//
//            } else if (currentCommand.contains("home")) {
//                performGlobalAction(GLOBAL_ACTION_HOME);
//            }
//
//
//            ArrayList<String> clickPhrase = new ArrayList<>(Arrays.asList("click", "tap", "touch", "press"));
//            ArrayList<String> scrollPhrase = new ArrayList<>(Arrays.asList("swipe", "scroll"));
//            ArrayList<String> enterPhrase = new ArrayList<>(Arrays.asList("enter", "type", "input"));
//            ArrayList<String> openPhrase = new ArrayList<>(Arrays.asList("open"));
//            String parameters = "";
//
//            Log.d("duc", Arrays.toString(words) + "");
//            if (words.length == 1) {
//                isExecuted = clickButtonByText(words[0].trim());
//            } else if (words.length > 1) {
//                for (int i = 1; i < words.length; i++) {
//                    parameters += words[i].toLowerCase(Locale.ROOT) + " ";
//                }
//                parameters = parameters.trim();
//                if (clickPhrase.contains(words[0].toLowerCase(Locale.ROOT))) {
//                    isExecuted = clickButtonByText(parameters);
//                    skipPreviousRTECheck = true;
//                } else if (scrollPhrase.contains(words[0].toLowerCase(Locale.ROOT))) {
//                    if (words[0].toLowerCase(Locale.ROOT).equals("scroll")) {
//                        isExecuted = scrollingActivity(parameters);
//                        skipPreviousRTECheck = true;
//                    } else {
//                        switch (parameters) {
//                            case "down":
//                                isExecuted = scrollingActivity("up");
//                                break;
//                            case "up":
//                                isExecuted = scrollingActivity("down");
//                                break;
//                            case "left":
//                                isExecuted = scrollingActivity("right");
//                                break;
//                            case "right":
//                                isExecuted = scrollingActivity("left");
//                                break;
//                        }
//
//                        skipPreviousRTECheck = true;
//                    }
//
//                } else if (openPhrase.contains(words[0].toLowerCase(Locale.ROOT))) {
//                    isExecuted = openApp(parameters);
//                    skipPreviousRTECheck = true;
//                } else if (enterPhrase.contains(words[0].toLowerCase(Locale.ROOT))) {
//                    setTextForAllSubNode(currentSource, 0, parameters);
//                    skipPreviousRTECheck = true;
//                    isExecuted = true;
//                }
//            }
//
//            if (isExecuted) {
//                Log.d("ss", "locally executed");
//                return;
//            }
//
//            String command = currentCommand + " ||| " + uiElements.toString().replace("[", "").replace("]", "");
//            Log.d("hihi", command);
//            Call<ResponseObject> call = jsonApi.getData(command);
//            call.enqueue(new Callback<ResponseObject>() {
//                @Override
//                public void onResponse(Call<ResponseObject> call, Response<ResponseObject> response) {
//                    if (!response.isSuccessful()) {
//                        assert response.errorBody() != null;
//                        Log.e("myErrTag", "not successful");
//                        return;
//                    }
//                    ResponseObject data = response.body();
//                    if (data != null && data.hypotheses != null && data.hypotheses.size() > 0) {
//
//                        Log.d("ggg", data.hypotheses.get(0).value);
//                        String rawData = data.hypotheses.get(0).value.replace(" ", "");
//                        String[] modelOutput = rawData.substring(1, rawData.length() - 1).split(",");
//                        if (modelOutput.length == 2) {
//                            String action = modelOutput[0].trim();
//                            String target = modelOutput[1].trim().replace("_", " ");
//
//                            if (target.startsWith("app:")) {
//                                target = target.substring(4);
//                            }
//                            switch (action) {
//                                case "PRESS":
//                                    clickButtonByText(target);
//                                    break;
//                                case "SWIPE":
//                                    scrollingActivity(target);
//                                    break;
//                                case "OPEN":
//                                    openApp(target);
//                                    break;
//                                case "ENTER":
//                                    setTextForAllSubNode(currentSource, 0, target);
//                                    break;
//                                default:
//                                    Log.d("duc", action + "/" + target);
//                            }
//                        } else if (modelOutput.length == 3) {
//                            String action = modelOutput[0].trim();
//                            String component = modelOutput[1].trim();
//                            String app = modelOutput[2].trim();
//
//                            if (app.startsWith("app:")) {
//                                app = app.substring(4);
//                            }
//
//                            if (component.startsWith("component:")) {
//                                component = component.substring(10);
//                            }
//                            invokeComponent(app, component);
//
//                        }
//
//                    }
//                }
//
//                @Override
//                public void onFailure(Call<ResponseObject> call, Throwable t) {
//                    Log.e("myErrTag", t.getMessage());
//                }
//            });
//
//
//            try {
//                java.util.concurrent.TimeUnit.SECONDS.sleep(1); // emulates thread synchronisation
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
// ***********commented out END

    public void getScrollableNode(AccessibilityNodeInfo currentNode) {
        /**
         * Get all the scrollable node in the current screen.
         * @param: currentNode: the current node that is being checked ( start from root node and recursively for all node)
         */
        if (currentNode == null) return;
        if (currentNode.isClickable()) {
            scrollableNodes.add(currentNode);
        }
        for (int i = 0; i < currentNode.getChildCount(); ++i) {
            getScrollableNode(currentNode.getChild(i));    // recursive call
        }
    }

    public String searchForTextView(AccessibilityNodeInfo currentNode, String allTexts) {
        String concatenatedString = allTexts;
        if (currentNode == null || concatenatedString.split(" ").length > 5)
            return concatenatedString;

        if (currentNode.getClassName() != null && currentNode.getClassName().equals("android.widget.TextView") && currentNode.getText() != null) {
            concatenatedString += currentNode.getText().toString() + " ";
        } else {
            for (int i = 0; i < currentNode.getChildCount(); ++i) {
                concatenatedString += searchForTextView(currentNode.getChild(i), concatenatedString);    // recursive call
            }
        }

        return concatenatedString;
    }


    public void setTextForAllSubNode(AccessibilityNodeInfo nodeInfo, int depth, String text) {
        /**
         * This function will set text for all sub-node ( all element on the screen)
         * @param: nodeInfo : current node that this function is called on ( will start from root node)
         * @param: depth : the current level of leaf
         * @param: text: the passed in text for writing in the edit text field.
         */
        if (nodeInfo == null) return;   // null check
        if (nodeInfo.isEditable()) {      // check if the node has editable field
            setGivenText(nodeInfo, text);       // call a method to put in the text
            Log.d("duc", nodeInfo + "");
        }
        for (int i = 0; i < nodeInfo.getChildCount(); ++i) {    // recursive call to reach all nested nodes/leaves
            setTextForAllSubNode(nodeInfo.getChild(i), depth + 1, text);
        }
    }

    public boolean scrollingActivity(String command) {
        /**
         * This function will work as scrolling the screen for user on invocation.
         * @param: a string - can be up or down specifying the scrolling direction.
         */
        boolean returnVal = false;
        getScrollableNode(currentSource);   // get all scrollable not within current screen
        if (scrollableNodes.size() == 0) {
            Log.d(debugLogTag, "Can't find item to scroll");
            return false;
        } else {      // if there exist item to be scrolled.
            for (AccessibilityNodeInfo node : scrollableNodes) {
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                // scrolling using gesture builder.
                final int height = displayMetrics.heightPixels;
                final int top = (int) (height * .25);
                final int mid = (int) (height * .5);
                final int bottom = (int) (height * .75);
                final int midX = displayMetrics.widthPixels / 2;
                final int width = displayMetrics.widthPixels;
                final int left = (int) (width * 0.25);
                final int right = (int) (width * 0.75);

                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                Path path = new Path();
                command = command.toLowerCase().trim();

                // Scroll up
                if (command.contains("up")) {
                    path.moveTo(midX, mid);
                    path.lineTo(midX, bottom);
                    returnVal = true;
                    // Scroll down
                } else if (command.contains("down")) {
                    path.moveTo(midX, mid);
                    path.lineTo(midX, top);
                    returnVal = true;
                } else if (command.contains("right")) {
                    path.moveTo(right, mid);
                    path.lineTo(left, mid);
                    returnVal = true;
                } else if (command.contains("left")) {
                    path.moveTo(left, mid);
                    path.lineTo(right, mid);
                    returnVal = true;
                }

                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 100, 300));
                dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) { // gesture execution
                        //Log.d(debugLogTag,"Gesture Completed");
                        super.onCompleted(gestureDescription);
                    }
                }, null);
            }
        }
        return returnVal;
    }

    @Override
    public void onInterrupt() {
        Log.d("Service Test", "Service Disconnected");
    }

    @Override
    protected void onServiceConnected() {
        /**
         * This function is invoked after the accessibility service has been stared by the user. this
         * function inflates the layout and draws the floating UI for the service. It also initialises
         * speech recognition & checks audio permissions.
         *
         * @param: None
         * @return: None
         * @post-cond: A button floating on top of the screen can be used to control the service
         *             by the user if the app have all the permissions it needs. Else opens settings
         *             page with the app's details.
         * */

        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        // add new
        loadAppNames();
        setupCommandMappings(); // Ensure mappings are set up when service connects
        //end
        Date date = new Date();
        currentTime = date.getTime();
        createSwitch();
        initializeSpeechRecognition();                      // Checking permissions & initialising speech recognition

        // v12 Testing using simulated voice
//        simulateVoiceCommand(R.raw.open_youtube, "open youtube");
//        simulateVoiceCommand(R.raw.swipe_up, "swipe up");
//        simulateVoiceCommand(R.raw.open_chrome, "open chrome");
//        simulateVoiceCommand(R.raw.open_fast_notepad, "open fast notepad");
//        simulateVoiceCommand(R.raw.open_settings, "open settings");

//        simulateVoiceCommand(R.raw.open_world_cuisines_show_me_view_videos_bookmarked, "open world cuisines show me view videos bookmarked");
//        simulateVoiceCommand(R.raw.open_world_cuisines_go_to_view_video_search, "open world cuisines go to view video search");
//        simulateVoiceCommand(R.raw.open_world_cuisines_show_me_view_favorite_detail, "open world cuisines show me view favorite detail");
//        simulateVoiceCommand(R.raw.open_world_cuisines_show_me_view_user_profile, "open world cuisines show me view user profile");
//        simulateVoiceCommand(R.raw.open_world_cuisines_show_me_view_recipe_store, "open world cuisines show me view recipe store");

        // v12 end
        loadData();
        loadAPIConnection();
        getDisplayMetrics();
        Log.d(debugLogTag, "Service Connected");
        //createText2VecModel();

        // testing voice commands by text
//        String testCommand = "open youtube";
//        String testCommand = "swipe up";
//        String testCommand = "open chrome";
//        String testCommand = "open fast notepad";
//        String testCommand = "open settings";

//        String testCommand = "open world cuisines show me VIEW Videos Bookmarked";
//        String testCommand = "open world cuisines go to VIEW Video Search";
//        String testCommand = "open world cuisines show me View favorite detail";
//        String testCommand = "open world cuisines show me view user profile";
//        String testCommand = "open world cuisines show me view recipe store";


//        String testCommand = "open youtube and search hi and hello";
//        String testCommand = "open maps and search ice cream";
//        String testCommand = "route to nearest burrito place";
//        String testCommand = "open camera and take picture";

//        String testCommand = "open maps and route to nearest icecream shop";
//        String testCommand = "open youtube open chrome";
//        String testCommand = "open youtube and go back";
//        String testCommand = "open youtube and go home";


//        executeCommand(testCommand); // This simulates the command input without using the microphone

        autoReloadHandler.post(runnable);
    }

    void loadAPIConnection() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:8099/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        jsonApi = retrofit.create(JsonApi.class);
    }

    void loadData() {

        mPrefs = getSharedPreferences(FILE_NAME, 0);
        Gson gson = new Gson();
        String json = mPrefs.getString("packageDataObjects", "");
        Type type = new TypeToken<ArrayList<PackageDataObject>>() {
        }.getType();
        packageDataObjects = gson.fromJson(json, type);
    }

    void deleteIntent() {
        Log.d("duc", currentOpeningFeature + "// " + currentOpeningPackage);
        if (packageDataObjects.size() > 0) {
            for (PackageDataObject packageDataObject : packageDataObjects) {
                if (packageDataObject.packageName.equals(currentOpeningPackage)) {
                    String[] dataArrIntent = new String[matchedIntents.size()];
                    dataArrIntent = matchedIntents.toArray(dataArrIntent);
                    String currentConcatenatedIntent = dataArrIntent[currentIntentIndex];
                    String[] intentComponent = currentConcatenatedIntent.split("  ");

                    ArrayList<String> intentActionList = packageDataObject.intentsByActivity.get(intentComponent[1]);
                    assert intentActionList != null;
                    boolean remove = intentActionList.remove(intentComponent[0]);
                    Log.d("duc", currentConcatenatedIntent);

                    packageDataObject.intentsByActivity.put(intentComponent[1], intentActionList);
                    if (remove) {
                        Log.d("duc", "Removed from the intent database");
                    }
                    SharedPreferences.Editor prefsEditor = mPrefs.edit();
                    Gson gson = new Gson();
                    String json = gson.toJson(packageDataObjects);
                    prefsEditor.putString("packageDataObjects", json);
                    prefsEditor.commit();
                    invokeComponent(currentAppName, currentOpeningFeature);
                    break;
                }
            }
        }

    }

    private void getDisplayMetrics() {
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;
    }


    private void createSwitch() {
        /**
         * This code will create a layout for the switch. This code is called whenever service is
         * connected and will be gone when service is shutdown
         *
         */

        // Check for permissions
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayout = new FrameLayout(this);

        // Create layout for switchBar
        switchBar = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        switchBar.gravity = Gravity.TOP;  // stick it to the top
        //WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |


        LayoutInflater inflater = LayoutInflater.from(this);
        View actionBar = inflater.inflate(R.layout.action_bar, mLayout);
        wm.addView(mLayout, switchBar);       // add it to the screen


        listenButton = mLayout.findViewById(R.id.listenBtn);
        // textMsg = mLayout.findViewById(R.id.msg);
        //  inputTxt = mLayout.findViewById(R.id.inputTxt);
        //  inputTxt.setBackgroundResource(R.color.black);
        listenButton.setBackgroundResource(R.drawable.start_btn);
        configureListenButton();

    }


    // This method is responsible for updating the switchBar coordniates upon touch and updating the view
    @Override
    public boolean onTouch(View view1, MotionEvent motionEvent) {

        switch (motionEvent.getAction()) {

            case MotionEvent.ACTION_DOWN:
                initialX = switchBar.x;
                initialY = switchBar.y;
                initialTouchX = motionEvent.getRawX();
                initialTouchY = motionEvent.getRawY();
                break;

            case MotionEvent.ACTION_UP:
                break;

            case MotionEvent.ACTION_MOVE:
                switchBar.x = initialX + (int) (motionEvent.getRawX() - initialTouchX);
                switchBar.y = initialY + (int) (motionEvent.getRawY() - initialTouchY);
                wm.updateViewLayout(mLayout, switchBar);
                break;
        }
        return false;
    }

    private void removeAllTooltips() {
        /**
         * This function will be called when something changed on the screen, reset all tooltips.
         *
         */
        for (TooltipRequiredNode tooltip : tooltipRequiredNodes) {    // remove the list of current tooltips
            if (tooltip.tooltipLayout != null)
                wm.removeView(tooltip.tooltipLayout);   // remove them from the screen
        }
        // reset all variables when changing to new screen.
        currentTooltipCount = 1;
        tooltipRequiredNodes.clear();
        foundLabeledNodes.clear();
    }

    private void configureListenButton() {
        /**
         * This function is called after the service has been connected. This function binds
         * functionality to the master button which can be used to turn on/off the tool.
         *
         * @param: None
         * @return: None
         * @post-cond: functionality has been added to the inflated button
         * */

        listenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listenButton.getText().toString().equalsIgnoreCase("start")) {
                    listenButton.setText("Stop");
                    listenButton.setBackgroundResource(R.drawable.stop_btn);
                    isVoiceCommandConnected = true;

                    // v11
                    speechStartTime = System.currentTimeMillis(); // Start timing
                    // v11
                    speechRecognizer.startListening(speechRecognizerIntent);       // on click listener to start listening audio
                } else {
                    listenButton.setText("Start");

                    listenButton.setBackgroundResource(R.color.transparent);
                    isVoiceCommandConnected = false;
                    listenButton.setBackgroundResource(R.drawable.start_btn);
                    speechRecognizer.stopListening();           // on click listener to stop listening & processing data
                    //         textMsg.setText("");
                }
            }
        });
    }


    private void configureConfirmButton(Button listenBtn) {

        listenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                deleteIntent();
            }
        });
    }

    // commented out old openApp()
//    private boolean openApp(String inputName) {
//        /**
//         * This function is used to check if the given string matches with any applications that the
//         * user may have installed. It launches apps that have matched. Current matching algorithm is
//         * trivial. (WIP: Improved Matching Algorithm)
//         *
//         * @param: inputName — This is a String that is supposed to be checked for app name matching
//         * @return: None
//         * @post-cond: Apps that match with the given string are launched and presented on the
//         *             foreground adding them to the system backstack if multiple apps are launched.
//         * */
//
//        final PackageManager pm = getPackageManager();
//        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA); // getting meta data of all installed apps
//
//        for (ApplicationInfo packageInfo : packages) {          // checking if the input has a match with app name
//            try {
//                ApplicationInfo info = pm.getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA);
//                String appName = (String) pm.getApplicationLabel(info).toString().toLowerCase();
//                if (appName.equals(inputName)) {
//                    Intent mIntent = getPackageManager().getLaunchIntentForPackage(
//                            packageInfo.packageName);
//                    if (mIntent != null) {
//                        try {
//                            startActivity(mIntent);
//                            return true;
//                            // Adding some text-to-speech feedback for opening apps based on input
//                            // Text-to-speech feedback if app not found);
////                            speakerTask(speechPrompt.get("open") + inputName);
//
//                        } catch (ActivityNotFoundException err) {
//                            // Text-to-speech feedback if app not found
////                            speakerTask(speechPrompt.get("noMatch") + inputName);
//
//                            // Render toast message on screen
//                            Toast t = Toast.makeText(getApplicationContext(),
//                                    "APP NOT FOUND", Toast.LENGTH_SHORT);
//                            t.show();
//                        }
//                    }
//                }
//            } catch (PackageManager.NameNotFoundException e) {
//                e.printStackTrace();                // handling app not found exception
////                speakerTask(speechPrompt.get("noMatch") + inputName);
//            }
//        }
//
//        return false;
//    }
    // end

    // add new openApp()
    private boolean openApp(String inputName) {
        // Convert input name to lowercase to match the stored names in appMap
        String appName = inputName.toLowerCase();

        // Check if appName exists in the HashMap (appMap)
        if (appMap.containsKey(appName)) {
            String packageName = appMap.get(appName);
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

            if (intent != null) {
                try {
                    startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "App not found", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "App not installed", Toast.LENGTH_SHORT).show();
        }

        return false;
    }
    // end


    // **********commented out, old loadAppNames()
//    private void loadAppNames() {
//        final PackageManager pm = getPackageManager();
//        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA); // getting meta data of all installed apps
//
//        for (ApplicationInfo packageInfo : packages) {          // checking if the input has a match with app name
//            try {
//                ApplicationInfo info = pm.getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA);
//                String appName = (String) pm.getApplicationLabel(info).toString().toLowerCase();
//                this.appNames.add(appName);
//            } catch (PackageManager.NameNotFoundException e) {
//                e.printStackTrace();                // handling app not found exception
//            }
//        }
//    }
    // ************ commented out end


    // add new loadAppNames() function, comment old one above
    private void loadAppNames() {
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA); // getting meta data of all installed apps

        for (ApplicationInfo packageInfo : packages) {          // checking if the input has a match with app name
            try {
                ApplicationInfo info = pm.getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA);
                String appName = (String) pm.getApplicationLabel(info).toString().toLowerCase();
                appMap.put(appName, packageInfo.packageName); // add new, replaced "this.appNames.add(appName); from original function"

                // Log when an app is added to appMap
                Log.d("AppMapLoad", "Added to map: " + appName + " -> " + packageInfo.packageName);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();                // handling app not found exception
            }
        }
        // Print the entire map after loading all apps
        Log.d("AppMapLoad", "Complete appMap: " + appMap.toString());
    }
    //end

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            String appName = intent.getStringExtra("app_name");
            String componentName = intent.getStringExtra("component_name");
            Log.d("duc", appName + " " + componentName);
            invokeComponent(appName, componentName);

        }

        return result;
    }

    public void speakerTask(String toSpeak) {
        /**
         * Use this method to call out to TTSService (Text-To-speech service) to speak out message
         * param: a string to be spoken by the Text-to-speech service
         */
        Intent i = new Intent(this, TTSService.class);
        i.putExtra("message", toSpeak);
        // starts service for intent
        startService(i);
    }

    boolean clickButtonByText(String word) {
        /**
         * This function will click a button (anything thats clickable) with provided information
         * param: word: a string to store data about what to click
         */
        // Processes input first to determine if number label was called
        // More efficient number label processing? Skips iterating through array of numbers and assumes the array is numerical order if input is a Digit
        Log.d("dddf", word);
        if (word.equals("")) {
            return false;
        }

        // check
        Log.d("dsds", foundLabeledNodes + "");
        for (LabelFoundNode node : foundLabeledNodes) {
            if (node.label.contains("burger")) {
                node.nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }

        if (TextUtils.isDigitsOnly(word)) {
            //Log.d(debugLogTag,word);
            if (Integer.parseInt(word) <= currentTooltipCount - 1) {
                if (tooltipRequiredNodes.size() >= Integer.parseInt(word) && tooltipRequiredNodes.get(Integer.parseInt(word) - 1).nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    //Log.d(debugLogTag, "Clicked number: " + word);    // log the information
                    return true;
                }

            }
        }

//        LabelFoundNode bestNode = null;
//        double maxScore = 0.5;
//        for(LabelFoundNode foundLabeledNode: foundLabeledNodes){
//            double concatenatedScore = 0;
//            for (String spokenWord : word.split(" ")) {
//                double bestMatch = 0;
//                for(String labelWord: foundLabeledNode.label.split(" ")){
//                    double currentScore = model.similarity(spokenWord,labelWord);
//                    if ((currentScore > bestMatch) && (currentScore > 0.85)) {
//                        bestMatch = currentScore;
//                    }
//                }
//                concatenatedScore += bestMatch;
//            }
//           if (concatenatedScore > maxScore) {
//               Log.d("ddd", concatenatedScore + " "+ maxScore + " " + foundLabeledNode.label + " " + word);
//               maxScore = concatenatedScore;
//               bestNode = foundLabeledNode;
//           }
//        }
//        if(bestNode != null){
//            if (bestNode.nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
//                Log.d("ddd", "Press " + bestNode.label);
//                //Log.d(debugLogTag, "Clicked on description:" + word);
//                return true;
//            }
//            // return once clicked
//        }

        if (currentSource == null) {
            return false;
        }

        for (LabelFoundNode foundLabeledNode : foundLabeledNodes) {
            Log.d("hehe", foundLabeledNode.label);
            if (foundLabeledNode.label.contains(word)) {

                if (foundLabeledNode.nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    //Log.d(debugLogTag, "Clicked on description:" + word);
                    return true;
                }
                // return once clicked
            }
        }

        //Find ALL of the nodes that match the "text" argument.
        List<AccessibilityNodeInfo> list = currentSource.findAccessibilityNodeInfosByText(word);    // find the node by text
        for (final AccessibilityNodeInfo node : list) { // go through each node to see if action can be performed
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;     // return once clicked
            }

        }
        // for some element that named with first capital word
        String camelCaseWord = word.substring(0, 1).toUpperCase() + word.substring(1);
        list = currentSource.findAccessibilityNodeInfosByText(camelCaseWord);    // find the node by text
        for (final AccessibilityNodeInfo node : list) { // go through each node to see if action can be performed
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;     // return once clicked
            }

        }


        return false;
    }

    public boolean commandExecution(String match) {
        /**
         * This function will be called on user voice input as a string of command
         * @param: match : the user command interpreted into a string
         */
        String[] words = match.split(" ");
        ArrayList<String> trimmedWords = new ArrayList<String>();
        // ["open","uber"...]
        //Log.d(debugLogTag,match);


        for (int index = 0; index < words.length; index++) {
            String word = words[index].toLowerCase().trim();
            trimmedWords.add(word);
        }
        boolean isActionInvoked = false;
        String initialWord = trimmedWords.get(0); // first word from the command
        if (initialWord.equals("back")) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            isActionInvoked = true;
        } else if (initialWord.equals("home")) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            isActionInvoked = true;
        } else if (launchTriggers.contains(initialWord)) {
            isActionInvoked = true;
            openApp(match);
        } else if (initialWord.equals("enter")) {
            String textToSet = match.substring(6);
            setTextForAllSubNode(currentSource, 0, textToSet);
            isActionInvoked = true;
        } else if (initialWord.equals("scroll")) {
            scrollingActivity(trimmedWords.get(1));
            isActionInvoked = true;
        } else if (!trimmedWords.get(0).equals("")) {
            if (clickButtonByText(trimmedWords.get(0))) {
                isActionInvoked = true;
            }
        }
        if (isActionInvoked) {
            if (isRecording) {
                savedCommands.add(match);
            }
            AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (manager.isEnabled()) {
                AccessibilityEvent e = AccessibilityEvent.obtain();
                e.setEventType(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
                e.setClassName(getClass().getName());
                e.getText().add("User interaction invoked this event");
                manager.sendAccessibilityEvent(e);
            }
            return true;
        }
        return false;
    }

    // Enter action
    public void setGivenText(AccessibilityNodeInfo currentNode, String text) {
        /**
         * This function will set the text for a given node
         * @param: currentNode: the node to store information about object that will be inserted the text.
         * @param: text: the customized passed in text to be written in the field.
         */
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo
                .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        currentNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    // new code
    String getAppComponent(PackageDataObject packageDataObject, String referencedName) {
        ArrayList<String> commonWords = new ArrayList<>(Arrays.asList("http", "deeplink", "com", "android", "intent", "action", "google"));
        List<String> appWords = Arrays.asList(packageDataObject.packageName.split("\\."));
        ArrayList<String> components = new ArrayList<>();
        for (String deeplink : packageDataObject.deepLinks) {
            String[] words = deeplink.split("[^\\w']+");
            String prefix = words[words.length - 1];
            if (!appWords.contains(prefix) && !commonWords.contains(prefix)) {
                components.add(prefix);
                if (referencedName.toLowerCase(Locale.ROOT).equals(prefix)) {
                    return deeplink;
                }
            }
        }
        for (String intent : packageDataObject.getQuerySearch("")) {
            String[] words = intent.split("[^\\w']+");
            ArrayList<String> keywords = new ArrayList<>();
            for (String word : words) {
                if (!appWords.contains(word) && !commonWords.contains(word)) {
                    if (word.endsWith("Activity")) {
                        //Log.d("nani", word);
                        keywords.add(word.replace("Activity", ""));
                    } else if (word.endsWith("Launcher")) {
                        //Log.d("nani", word);
                        keywords.add(word.replace("Launcher", ""));
                    } else if (word.contains("_")) {
                        //Log.d("nani", word);
                        word.replace("_", " ");
                    } else {
                        keywords.add(word);
                    }

                }
            }
            ArrayList<String> finalSet = new ArrayList<>();
            for (String keyword : keywords) {
                String[] singleWords = keyword.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
                String element = String.join(" ", singleWords);
                finalSet.add(element);
            }
            String componentName = String.join(" ", finalSet);
            components.add(componentName);
            if (componentName.toLowerCase(Locale.ROOT).equals(referencedName)) {
                return intent;
            }
        }
        Log.d("Test", packageDataObject.name + ": " + components);
        return "!@#!@#@!#!@#";
    }

    public void invokeComponent(String appSearchString, String featureSearchString) {
        appSearchString = appSearchString.replace("_", " ");
        featureSearchString = featureSearchString.replace("_", " ");
        boolean isAppFound = false;
        String componentOutput = "";
        PackageDataObject currentPackage = null;
        for (PackageDataObject packageDataObject : packageDataObjects) {

            if (packageDataObject.name.replace("!", "").equals(appSearchString)) {
                componentOutput = getAppComponent(packageDataObject, featureSearchString);
                isAppFound = true;
                currentPackage = packageDataObject;
                break;
            }


        }
        if (isAppFound) {
            if (currentPackage.getDeepLinks().contains(componentOutput)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(componentOutput));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // Define what your app should do if no activity can handle the intent.

                    Log.d("Invocation Errors", e.getMessage());
                }
            } else if (currentPackage.getQuerySearch("").contains(componentOutput)) {
                String[] intents = componentOutput.split("  ");
                Intent intent = new Intent(intents[0]);
                intent.setComponent(new ComponentName(currentPackage.packageName, intents[1]));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                try {
                PackageManager packageManager = getPackageManager();

                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent);
//                        intentValidationHandler.postDelayed(intentExecutionValidation, 4000);
//                        Log.d("duc", componentOutput + "1111" + currentOpeningPackage + "11111" + intents[1]);
                }
//                    else {
//                        Log.d("Duc", "No Intent available to handle action");
//                        currentIntentIndex += 1;
//                        invokeComponent(appSearchString, featureSearchString);
//                    }


//                } catch (Exception e) {
//                    currentIntentIndex += 1;
//                    // Define what your app should do if no activity can handle the intent
//                    Log.d("Duc", e.getMessage());
//                    invokeComponent(appSearchString, featureSearchString);
//                }
            } else {
                Intent mIntent = getPackageManager().getLaunchIntentForPackage(
                        currentPackage.packageName);

                if (mIntent != null) {
                    try {
                        Log.d("Duc", "Open app launcher screen instead");
                        startActivity(mIntent);
                        speakerTask("Component not found, opened the app instead");
                        // Adding some text-to-speech feedback for opening apps based on input
                        // Text-to-speech feedback if app not found);
                        //                            speakerTask(speechPrompt.get("open") + inputName);

                    } catch (ActivityNotFoundException err) {
                        // Text-to-speech feedback if app not found
                        //                            speakerTask(speechPrompt.get("noMatch") + inputName);

                        // Render toast message on screen
                        Toast t = Toast.makeText(getApplicationContext(),
                                "APP NOT FOUND", Toast.LENGTH_SHORT);
                        speakerTask("Cannot open the app");
                        t.show();
                    }
//                    currentIntentIndex = 0;
                }
            }
        }


    }

    public boolean isTransitionToNewApp(String packageName) {

        Log.d("duc", packageName + '/' + getRootInActiveWindow().getPackageName());
        return packageName == currentSource.getPackageName();

    }


    Handler intentValidationHandler = new Handler();
    final Runnable intentExecutionValidation = new Runnable() {
        public void run() {
            Log.d("duc", "Handler " + getRootInActiveWindow().getPackageName());
            Log.d("duc", "Handler " + currentOpeningPackage);
            if (currentOpeningPackage.equals(getRootInActiveWindow().getPackageName())) {

                Log.d("duc", "success");
                speakerTask("Opened the component");
            } else {
                currentIntentIndex += 1;
                currentIntentIndex += 1;
                Log.d("duc", "handler reinvoke");
                invokeComponent(currentAppName, currentOpeningFeature);
            }

        }
    };

    public static void verifyStoragePermission(Activity activity) {
        final int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        // Get permission status
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission we request it
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
