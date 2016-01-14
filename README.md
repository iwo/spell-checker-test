# spell-checker-test
Test rig comparing Android spell checker with Hunspell

[Blog post](http://www.iwobanas.com/2016/01/how-bad-is-android-google-keyboard/) describing this test.

Build and run with Android Studio or Gradle.

## Before running the test
 * extract [hunspell_words.zip](hunspell_words.zip) to external storage on your device
 * or generate your own words list using [generate_word_list.sh](generate_word_list.sh) script
 * enable all the languages you want to test in your Android Keyboard and make sure it downloaded the dictionaries
 * modify [MainActivity.java](app/src/main/java/iwobanas/com/spellcheckertest/MainActivity.java) to reflect the list of languages you want to test

## Test results
Summary of the test is stored in `spell_checker_test.csv` file on external storage.
Lists of recognized and unrecognized words are stored in `recognized_{language}.txt` and `unrecognized_{language}.txt` respectively.
