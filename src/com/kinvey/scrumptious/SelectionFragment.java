/** 
 * Copyright (c) 2013, Kinvey, Inc. All rights reserved.
 *
 * This software contains valuable confidential and proprietary information of
 * KINVEY, INC and is subject to applicable licensing agreements.
 * Unauthorized reproduction, transmission or distribution of this file and its
 * contents is a violation of applicable laws.
 * 
 */

package com.kinvey.scrumptious;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.facebook.*;
import com.facebook.model.*;
import com.kinvey.scrumptious.R;
import com.facebook.widget.ProfilePictureView;
import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyUserCallback;
import com.kinvey.java.User;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.core.MediaHttpUploader;
import com.kinvey.java.core.UploaderProgressListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Fragment that represents the main selection screen for Scrumptious.
 */
public class SelectionFragment extends Fragment {

    private static final String TAG = "SelectionFragment";
    private static final String POST_ACTION_PATH = "me/fb_sample_scrumps:eat";
    private static final String PENDING_ANNOUNCE_KEY = "pendingAnnounce";
    private static final Uri M_FACEBOOK_URL = Uri.parse("http://m.facebook.com");

    private static final int REAUTH_ACTIVITY_CODE = 100;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private Button announceButton;
    private ListView listView;
    private ProgressDialog progressDialog;
    private List<BaseListElement> listElements;
    private ProfilePictureView profilePictureView;
    private TextView userNameView;
    private boolean pendingAnnounce;
    private Client kinveyClient;
    private String imagePath;
    private String imageName;
    
    private EatAction eatAction = GraphObject.Factory.create(EatAction.class);

    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
        kinveyClient = new Client.Builder(getActivity()).build();
        
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.selection, container, false);

        profilePictureView = (ProfilePictureView) view.findViewById(R.id.selection_profile_pic);
        profilePictureView.setCropped(true);
        userNameView = (TextView) view.findViewById(R.id.selection_user_name);
        announceButton = (Button) view.findViewById(R.id.announce_button);
        listView = (ListView) view.findViewById(R.id.selection_list);

        announceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleAnnounce();
            }
        });
        init(savedInstanceState);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REAUTH_ACTIVITY_CODE) {
            uiHelper.onActivityResult(requestCode, resultCode, data);
        } else if (resultCode == Activity.RESULT_OK && requestCode >= 0 && requestCode < listElements.size()) {
            listElements.get(requestCode).onActivityResult(data);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        for (BaseListElement listElement : listElements) {
            listElement.onSaveInstanceState(bundle);
        }
        bundle.putBoolean(PENDING_ANNOUNCE_KEY, pendingAnnounce);
        uiHelper.onSaveInstanceState(bundle);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    /**
     * Notifies that the session token has been updated.
     */
    private void tokenUpdated() {
        if (pendingAnnounce) {
            handleAnnounce();
        }
    }

    private void onSessionStateChange(final Session session, SessionState state, Exception exception) {
        if (session != null && session.isOpened()) {
            if (state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
                tokenUpdated();
            } else {
                makeMeRequest(session);
            }
        }
    }

    private void makeMeRequest(final Session session) {
        Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser user, Response response) {
                if (session == Session.getActiveSession()) {
                    if (user != null) {
                        profilePictureView.setProfileId(user.getId());
                        userNameView.setText(user.getName());
                    }
                }
                if (response.getError() != null) {
                    handleError(response.getError());
                }
            }
        });
        request.executeAsync();

    }

    /**
     * Resets the view to the initial defaults.
     */
    private void init(Bundle savedInstanceState) {
        announceButton.setEnabled(false);

        listElements = new ArrayList<BaseListElement>();

        listElements.add(new EatListElement(0));
        listElements.add(new LocationListElement(1));
        listElements.add(new PeopleListElement(2));
        listElements.add(new PictureListElement(3));

        if (savedInstanceState != null) {
            for (BaseListElement listElement : listElements) {
                listElement.restoreState(savedInstanceState);
            }
            pendingAnnounce = savedInstanceState.getBoolean(PENDING_ANNOUNCE_KEY, false);
        }

        listView.setAdapter(new ActionListAdapter(getActivity(), R.id.selection_list, listElements));

        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            makeMeRequest(session);
            kinveyClient.user().loginFacebook(session.getAccessToken(), new KinveyUserCallback() {

				@Override
				public void onFailure(Throwable t) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onSuccess(User user) {
					// TODO Auto-generated method stub	
				}
            });
        }
    }

    private void handleAnnounce() {
        pendingAnnounce = false;
        
        for (BaseListElement element : listElements) {
            element.populateOGAction(eatAction);
        }
        
        Session session = Session.getActiveSession();
        
        // Show a progress dialog because sometimes the requests can take a while.
        progressDialog = ProgressDialog.show(getActivity(), "",
                getActivity().getResources().getString(R.string.progress_dialog_text), true);
        
        if (!kinveyClient.user().isUserLoggedIn() && session != null) {
        	kinveyClient.user().loginFacebook(session.getAccessToken(), new KinveyUserCallback() {

				@Override
				public void onFailure(Throwable t) {
					dismissProgressListener();
					Log.e(TAG, "Login Failure");
					Toast.makeText(getActivity(), "Failed to login", Toast.LENGTH_LONG).show();
				}

				@Override
				public void onSuccess(User result) {
					Log.i(TAG, "User logged into Kinvey");
					sendPicture();
					
				}
        		
        	});
        } else {
        	sendPicture();
        }
        		



    }
    
    private void sendPicture() {
    	File imageFile = new File(imagePath);
    	imageName = imageFile.getName();
    	kinveyClient.file().upload(imageFile, new UploaderProgressListener() {

			@Override
			public void onFailure(Throwable t) {
				dismissProgressListener();
				Log.e(TAG, "Upload Failure", t);
				Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_LONG).show();
			}

			@Override
			public void onSuccess(Void arg0) {
				saveEntity();
			}

			@Override
			public void progressChanged(MediaHttpUploader uploader)
					throws IOException {
				Log.i(TAG, "Upload Progress: "+ Double.toString(uploader.getProgress()));
				
			}
    		
    	});
    	
    }

	private void saveEntity() {
		MealEntity meal = new MealEntity();
		
        meal.setSelectedMeal(eatAction.getProperty("foodChoice").toString());
        meal.setDeterminer(eatAction.getProperty("foodDeterminer").toString());
        meal.setImageURL(imageName);
        List<GraphObject> tagList = eatAction.getTags();
        String[] tagJson = new String[tagList.size()];
        String tagIds[] = new String[tagList.size()];
        
        for (int i = 0; i < tagList.size(); i++) {
        	tagJson[i] = tagList.get(i).getInnerJSONObject().toString().replace("\\", "");
        	tagIds[i] = tagList.get(i).getProperty("id").toString();
        }
        final String[] tags = tagIds;
        meal.setTags(tagJson);
        
        double latitude =  eatAction.getPlace().getLocation().getLatitude();
        double longitude = eatAction.getPlace().getLocation().getLongitude();
        
        meal.set_geoloc(latitude,longitude);
        meal.setLatitude(latitude);
        meal.setLongitude(longitude);
        meal.setPlace(eatAction.getPlace().getName());
        
		kinveyClient.appData("Eating", MealEntity.class).save(meal, new KinveyClientCallback<MealEntity>() {

			@Override
			public void onFailure(Throwable t) {
				dismissProgressListener();
				Log.e(TAG, "Failed to save entity");
				Toast.makeText(getActivity(),"Failed to save data", Toast.LENGTH_LONG).show();
				
			}

			@Override
			public void onSuccess(MealEntity result) {
				postToOG(result.get_id(), tags);
				
			}
        	
        });
	}
    
    private void postToOG(String id, String[] tags) {
    	OpenGraphEntity ogPost = new OpenGraphEntity();
    	ogPost.set_id("kinvey_scrumptious:meal");
    	ogPost.setEntityId(id);
    	ogPost.setTags(tags);
    	kinveyClient.appData("kinvey_scrumptious:eat", OpenGraphEntity.class).save(ogPost, new KinveyClientCallback<OpenGraphEntity>() {

			@Override
			public void onFailure(Throwable t) {
				Log.e(TAG, "Push to OpenGraph failed");
				dismissProgressListener();
				Toast.makeText(getActivity(), "Post to OpenGraph Failure.", Toast.LENGTH_LONG).show();
				
			}

			@Override
			public void onSuccess(OpenGraphEntity result) {
				Log.i(TAG, "Push to OpenGraph success");
				dismissProgressListener();
				Toast.makeText(getActivity(), "Post to OpenGraph Success", Toast.LENGTH_LONG).show();
			}

			
    		
    	});
    }
    
    private void dismissProgressListener() {
		if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
	}

    private void requestPublishPermissions(Session session) {
        if (session != null) {
            Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, PERMISSIONS)
                    // demonstrate how to set an audience for the publish permissions,
                    // if none are set, this defaults to FRIENDS
                    .setDefaultAudience(SessionDefaultAudience.FRIENDS)
                    .setRequestCode(REAUTH_ACTIVITY_CODE);
            session.requestNewPublishPermissions(newPermissionsRequest);
        }
    }
    
    private void handleError(FacebookRequestError error) {
        DialogInterface.OnClickListener listener = null;
        String dialogBody = null;

        if (error == null) {
            dialogBody = getString(R.string.error_dialog_default_text);
        } else {
            switch (error.getCategory()) {
                case AUTHENTICATION_RETRY:
                    // tell the user what happened by getting the message id, and
                    // retry the operation later
                    String userAction = (error.shouldNotifyUser()) ? "" :
                            getString(error.getUserActionMessageId());
                    dialogBody = getString(R.string.error_authentication_retry, userAction);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, M_FACEBOOK_URL);
                            startActivity(intent);
                        }
                    };
                    break;

                case AUTHENTICATION_REOPEN_SESSION:
                    // close the session and reopen it.
                    dialogBody = getString(R.string.error_authentication_reopen);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Session session = Session.getActiveSession();
                            if (session != null && !session.isClosed()) {
                                session.closeAndClearTokenInformation();
                            }
                        }
                    };
                    break;

                case PERMISSION:
                    // request the publish permission
                    dialogBody = getString(R.string.error_permission);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            pendingAnnounce = true;
                            requestPublishPermissions(Session.getActiveSession());
                        }
                    };
                    break;

                case SERVER:
                case THROTTLING:
                    // this is usually temporary, don't clear the fields, and
                    // ask the user to try again
                    dialogBody = getString(R.string.error_server);
                    break;

                case BAD_REQUEST:
                    // this is likely a coding error, ask the user to file a bug
                    dialogBody = getString(R.string.error_bad_request, error.getErrorMessage());
                    break;

                case OTHER:
                case CLIENT:
                default:
                    // an unknown issue occurred, this could be a code error, or
                    // a server side issue, log the issue, and either ask the
                    // user to retry, or file a bug
                    dialogBody = getString(R.string.error_unknown, error.getErrorMessage());
                    break;
            }
        }

        new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.error_dialog_button_text, listener)
                .setTitle(R.string.error_dialog_title)
                .setMessage(dialogBody)
                .show();
    }

    private void startPickerActivity(Uri data, int requestCode) {
        Intent intent = new Intent();
        intent.setData(data);
        intent.setClass(getActivity(), PickerActivity.class);
        startActivityForResult(intent, requestCode);
    }

    /**
     * Interface representing the Meal Open Graph object.
     */
    private interface MealGraphObject extends GraphObject {
        public String getUrl();
        public void setUrl(String url);

        public String getId();
        public void setId(String id);
    }

    /**
     * Interface representing the Eat action.
     */
    private interface EatAction extends OpenGraphAction {
        public MealGraphObject getMeal();
        public void setMeal(MealGraphObject meal);
    }

    /**
     * Used to inspect the response from posting an action
     */
    private interface PostResponse extends GraphObject {
        String getId();
    }

    private class EatListElement extends BaseListElement {

        private static final String FOOD_KEY = "food";
        private static final String FOOD_DETERMINER_KEY = "food_determiner";

        private final String[] foodChoices;
        private final String[] foodDeterminers;
        private String foodChoiceDeterminer = null;
        private String foodChoice = null;

        public EatListElement(int requestCode) {
            super(getActivity().getResources().getDrawable(R.drawable.action_eating),
                  getActivity().getResources().getString(R.string.action_eating),
                  getActivity().getResources().getString(R.string.action_eating_default),
                  requestCode);
            foodChoices = getActivity().getResources().getStringArray(R.array.food_types);
            foodDeterminers = getActivity().getResources().getStringArray(R.array.food_determiners);
        }

        @Override
        protected View.OnClickListener getOnClickListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMealOptions();
                }
            };
        }

        @Override
        protected void populateOGAction(OpenGraphAction action) {
            if (foodChoice != null) {
                action.setProperty("foodChoice", foodChoice);
                action.setProperty("foodDeterminer",foodChoiceDeterminer);
            }
        }

        @Override
        protected void onSaveInstanceState(Bundle bundle) {
            if (foodChoice != null) {
                bundle.putString(FOOD_KEY, foodChoice);
                bundle.putString(FOOD_DETERMINER_KEY, foodChoiceDeterminer);
            }
        }

        @Override
        protected boolean restoreState(Bundle savedState) {
            String food = savedState.getString(FOOD_KEY);
            String foodUrl = savedState.getString(FOOD_DETERMINER_KEY);
            if (food != null && foodUrl != null) {
                foodChoice = food;
                foodChoiceDeterminer = foodUrl;
                setFoodText();
                return true;
            }
            return false;
        }

        private void showMealOptions() {
            String title = getActivity().getResources().getString(R.string.select_meal);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(title).
                    setCancelable(true).
                    setItems(foodChoices, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            foodChoice = foodChoices[i];
                            foodChoiceDeterminer = foodDeterminers[i];
                            setFoodText();
                            notifyDataChanged();
                        }
                    });
            builder.show();
        }

        private void setFoodText() {
            if (foodChoice != null) {
                setText2(foodChoice);
                announceButton.setEnabled(true);
            } else {
                setText2(getActivity().getResources().getString(R.string.action_eating_default));
                announceButton.setEnabled(false);
            }
        }
    }

    private class PeopleListElement extends BaseListElement {

        private static final String FRIENDS_KEY = "friends";

        private List<GraphUser> selectedUsers;

        public PeopleListElement(int requestCode) {
            super(getActivity().getResources().getDrawable(R.drawable.action_people),
                  getActivity().getResources().getString(R.string.action_people),
                  getActivity().getResources().getString(R.string.action_people_default),
                  requestCode);
        }

        @Override
        protected View.OnClickListener getOnClickListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startPickerActivity(PickerActivity.FRIEND_PICKER, getRequestCode());
                }
            };
        }

        @Override
        protected void onActivityResult(Intent data) {
            selectedUsers = ((ScrumptiousApplication) getActivity().getApplication()).getSelectedUsers();
            setUsersText();
            notifyDataChanged();
        }

        @Override
        protected void populateOGAction(OpenGraphAction action) {
            if (selectedUsers != null) {
                action.setTags(selectedUsers);
            }
        }

        @Override
        protected void onSaveInstanceState(Bundle bundle) {
            if (selectedUsers != null) {
                bundle.putByteArray(FRIENDS_KEY, getByteArray(selectedUsers));
            }
        }

        @Override
        protected boolean restoreState(Bundle savedState) {
            byte[] bytes = savedState.getByteArray(FRIENDS_KEY);
            if (bytes != null) {
                selectedUsers = restoreByteArray(bytes);
                setUsersText();
                return true;
            }
            return false;
        }

        private void setUsersText() {
            String text = null;
            if (selectedUsers != null) {
                if (selectedUsers.size() == 1) {
                    text = String.format(getResources().getString(R.string.single_user_selected),
                            selectedUsers.get(0).getName());
                } else if (selectedUsers.size() == 2) {
                    text = String.format(getResources().getString(R.string.two_users_selected),
                            selectedUsers.get(0).getName(), selectedUsers.get(1).getName());
                } else if (selectedUsers.size() > 2) {
                    text = String.format(getResources().getString(R.string.multiple_users_selected),
                            selectedUsers.get(0).getName(), (selectedUsers.size() - 1));
                }
            }
            if (text == null) {
                text = getResources().getString(R.string.action_people_default);
            }
            setText2(text);
        }

        private byte[] getByteArray(List<GraphUser> users) {
            // convert the list of GraphUsers to a list of String where each element is
            // the JSON representation of the GraphUser so it can be stored in a Bundle
            List<String> usersAsString = new ArrayList<String>(users.size());

            for (GraphUser user : users) {
                usersAsString.add(user.getInnerJSONObject().toString());
            }
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                new ObjectOutputStream(outputStream).writeObject(usersAsString);
                return outputStream.toByteArray();
            } catch (IOException e) {
                Log.e(TAG, "Unable to serialize users.", e);
            }
            return null;
        }

        private List<GraphUser> restoreByteArray(byte[] bytes) {
            try {
                @SuppressWarnings("unchecked")
                List<String> usersAsString =
                        (List<String>) (new ObjectInputStream(new ByteArrayInputStream(bytes))).readObject();
                if (usersAsString != null) {
                    List<GraphUser> users = new ArrayList<GraphUser>(usersAsString.size());
                    for (String user : usersAsString) {
                        GraphUser graphUser = GraphObject.Factory
                                .create(new JSONObject(user), GraphUser.class);
                        users.add(graphUser);
                    }
                    return users;
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Unable to deserialize users.", e);
            } catch (IOException e) {
                Log.e(TAG, "Unable to deserialize users.", e);
            } catch (JSONException e) {
                Log.e(TAG, "Unable to deserialize users.", e);
            }
            return null;
        }
    }

    private class LocationListElement extends BaseListElement {

        private static final String PLACE_KEY = "place";

        private GraphPlace selectedPlace = null;

        public LocationListElement(int requestCode) {
            super(getActivity().getResources().getDrawable(R.drawable.action_location),
                  getActivity().getResources().getString(R.string.action_location),
                  getActivity().getResources().getString(R.string.action_location_default),
                  requestCode);
        }

        @Override
        protected View.OnClickListener getOnClickListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startPickerActivity(PickerActivity.PLACE_PICKER, getRequestCode());
                }
            };
        }

        @Override
        protected void onActivityResult(Intent data) {
            selectedPlace = ((ScrumptiousApplication) getActivity().getApplication()).getSelectedPlace();
            setPlaceText();
            notifyDataChanged();
        }

        @Override
        protected void populateOGAction(OpenGraphAction action) {
            if (selectedPlace != null) {
                action.setPlace(selectedPlace);
            }
        }

        @Override
        protected void onSaveInstanceState(Bundle bundle) {
            if (selectedPlace != null) {
                bundle.putString(PLACE_KEY, selectedPlace.getInnerJSONObject().toString());
            }
        }

        @Override
        protected boolean restoreState(Bundle savedState) {
            String place = savedState.getString(PLACE_KEY);
            if (place != null) {
                try {
                    selectedPlace = GraphObject.Factory
                            .create(new JSONObject(place), GraphPlace.class);
                    setPlaceText();
                    return true;
                } catch (JSONException e) {
                    Log.e(TAG, "Unable to deserialize place.", e);
                }
            }
            return false;
        }

        private void setPlaceText() {
            String text = null;
            if (selectedPlace != null) {
                text = selectedPlace.getName();
            }
            if (text == null) {
                text = getResources().getString(R.string.action_location_default);
            }
            setText2(text);
        }

    }
    
    private class PictureListElement extends BaseListElement {

        private static final String PICTURE_KEY = "picture";

        private com.facebook.model.GraphPlace selectedPlace = null;

        private boolean _taken = false;
        private Bitmap bitmap;

        public PictureListElement(int requestCode) {
            super(getActivity().getResources().getDrawable(R.drawable.action_camera),
                  getActivity().getResources().getString(R.string.action_picture),
                  getActivity().getResources().getString(R.string.action_picture_default),
                  requestCode);
        }

        @Override
        protected View.OnClickListener getOnClickListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                	
                    // Create an image file name
                    String timeStamp = UUID.randomUUID().toString();
                    String imageFileName = "Scrumptious" + timeStamp + "_";
                    File outputDir = new File(Environment.getExternalStoragePublicDirectory(
        		            Environment.DIRECTORY_PICTURES), "scrumptious");
                    if (!outputDir.exists()) {
                    	boolean result = outputDir.mkdir();
                    	Toast.makeText(getActivity(), Boolean.toString(result), Toast.LENGTH_LONG).show();
                    }
                    
                    File image = null;
					try {
						image = File.createTempFile(
						    imageFileName, 
						    ".jpg", 
						    outputDir
						);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Log.e(TAG,"IO Exception",e);
					}
                    
                	image.mkdirs();
                	imagePath = image.getAbsolutePath().toString();
                    Uri outputFileUri = Uri.fromFile( image );
                	
                    // TODO:  Check if Camera functionality exists
                    
                	Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                	takePictureIntent.putExtra( MediaStore.EXTRA_OUTPUT, outputFileUri );
                	takePictureIntent.putExtra("return-data", true);
                    startActivityForResult(takePictureIntent, getRequestCode());
                }
            };
        }

        @Override
        protected void onActivityResult(Intent intent) {
            onPhotoTaken();
            notifyDataChanged();
        }
        
        protected void onPhotoTaken()
        {
            _taken = true;
            
     
            	
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            	
            bitmap = BitmapFactory.decodeFile( imagePath, options );
            Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, 64, 64, false);
            this.setIcon(thumbnail);
        }

        @Override
        protected void populateOGAction(OpenGraphAction action) {
            
        }

        @Override
        protected void onSaveInstanceState(Bundle bundle) {
            if (bitmap != null) {
                bundle.putParcelable("BITMAP", bitmap);
            }
        }

        @Override
        protected boolean restoreState(Bundle savedState) {
            Bitmap picture = (Bitmap) savedState.getParcelable(PICTURE_KEY);
            if (picture != null) {
                bitmap = picture;
                return true;
            }
            return false;
        }
    }

    private class ActionListAdapter extends ArrayAdapter<BaseListElement> {
        private List<BaseListElement> listElements;

        public ActionListAdapter(Context context, int resourceId, List<BaseListElement> listElements) {
            super(context, resourceId, listElements);
            this.listElements = listElements;
            for (int i = 0; i < listElements.size(); i++) {
                listElements.get(i).setAdapter(this);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater =
                        (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.listitem, null);
            }

            BaseListElement listElement = listElements.get(position);
            if (listElement != null) {
                view.setOnClickListener(listElement.getOnClickListener());
                ImageView icon = (ImageView) view.findViewById(R.id.icon);
                TextView text1 = (TextView) view.findViewById(R.id.text1);
                TextView text2 = (TextView) view.findViewById(R.id.text2);
                if (icon != null) {
                    icon.setImageDrawable(listElement.getIcon());
                }
                if (text1 != null) {
                    text1.setText(listElement.getText1());
                }
                if (text2 != null) {
                    text2.setText(listElement.getText2());
                }
            }
            return view;
        }

    }
}
