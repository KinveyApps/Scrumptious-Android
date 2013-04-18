Scrumptious (Kinvey)
=====
This sample code shows how to modify Facebook's [Scrumptious Sample App](https://github.com/facebook/facebook-android-sdk/tree/master/samples/Scrumptious) to use Kinvey to host Open Graph Objects and post actions to a user's timeline. Kinvey dynamically generates the Open Graph object html based upon the information chosen by the user. 

Scrumptious users post that they "ate" a meal, allowing them to tag where, when, and with whom they ate the meal, and attach a picture of the meal. 

## Using the Sample
The sample repository comes with the Kinvey Android Client and Fracebook frameworks that it was developed against. In production code, you should update to the latest versions of these libraries.

* [Download Kinvey Library](http://devcenter.kinvey.com/android/downloads)
* [Download Facebook SDK](http://developers.facebook.com/ios/downloads/)

### Set-up the Backend
1. Create your Scrumptious App on Facebook.
    * Set up the "eat" action and "meal" object.
    * App namespace is `kinvey_scrumptious`.
2. Create a new App on [Kinvey](https://console.kinvey.com/).
    1. Create a "Eating" collection to store the data for each meal uploaded by the users.
    2. Set up mappings in the "Data Links" -> "Facebook Open Graph" settings. Follow the steps in [this tutorial](http://devcenter.kinvey.com/android/tutorials/facebook-opengraph-tutorial) set up the mappings between the Kinvey object and the Facebook object.
         * You'll need to paste the "get code" for the meal object. This will set up some of the fields and settings for a `kinvey_scrumptious:meal` object type.
         * Map the following fields:
         	* `og:title` -> `selectedMeal`
         	* `og:image` -> `imageURL`
         	* `place:location:latitude` -> `latitude`
         	* `place:location:longitude` -> `longitude`
         * You will need to add additional mappings for these fields:
            * `og:determiner` -> `determiner`          

    3. Add a new action `kinvey_scrumptious:eat` to represent the eat action.

### Set-up the App
1. In `assets/kinvey.properties` enter your Kinvey app __App ID__ and __App Secret__.
2. In `res\strings.xml`, enter your __Facebook App ID__ in the `app_id` value.

## Modifications to the Original Scrumptious
1. Created MealEntity object to represent the OG meal object. This is used to store the meal's information in the Kinvey backend.
2. MealEntity objects are populated with data chosen by the user in the interface, and then uploaded to Kinvey in three separate steps:
    1. Upload the image to Kinvey.
    2. Upload the data to Kinvey.
    3. Tell Kinvey to post the `eat` action to the user's timeline.
3. Added ability to take a picture of the meal.
4. Added additional OG fields, such as `determiner`, to improve the user experience.


## Extending Scrumptious
* To add new meal types, just add the name to the `food_types` array created in `res\strings.xml`. You will also need to add a determiner to the `food_determiners` array at the same index. The determiner is the English indefinite article that corresponds to the meal name. This is used to make the OG action read like a normal sentence. For example "Bob ate _a_ Hotdog", "Jill ate _an_ Escargot", "Roger ate Mexican".
* To add new fields, add a property to the `MealEntity` and map that property to the backend by preceding the variable declaration with a `@Key` annotation. Then in the `FBOG` collection on the backend, map the field name to the Facebook Open Graph object field name. 

## Contact
Website: [www.kinvey.com](http://www.kinvey.com)

Support: [support@kinvey.com](http://docs.kinvey.com/mailto:support@kinvey.com)