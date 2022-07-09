# CRYPTILE

## What is the purpose of this app?

This is an encryption application. it would take your files and encrypt them so that only you can see them. This can be useful when you have files that are important but, at the same time you want them accessible at all times. With CRYPTILE, you don't have to worry about having your data stolen. Since the only place the data will ever exist is on your physical device, no worry about any cloud provider having your data, neither of any other app being able to read it. It would be password locked and wouldn't be in readabe format for anybody without the password/s. A safe is a term that would be a collection of such files.

### An example use case -

Maybe you want some of your important buisness documents always with you. Here, you can create a safe with the name `buisness`. This would help you identify the safe's purpose. Similiarly, maybe you want to store your bank's papers. you can name the safe `finance`. This would again similiarly help you identify the safe's purpose.

<hr>

## Documentation

### Sign Up -

You can either sign up using the Google account Sign up process or your email address. Signing up to the app creates your account name and a secret key. This key's use case will be discussed later on during the creation of a safe

### Creating a CRYPTILE safe -

Creating a Safe is quite easy. Once on the main screen click the circular `+` button. This will navigate you to the `create safe` screen. In there, all you have to do is provide the application with the name of the safe, the password you want to give it. Now, here, you can use an additional password if you so require. Next you can also provide the safe with its own separate location. the default location is provided and changing that is optional. You can change the location using the image button on the right of the card. Select the folder where you want to create the safe. After selection, the path value should be updated in the app. Next up is the `Personal Access Only?` option. Enabling this option means only you can access the safe. When this option is enabled, the safe adds another layer of encryption using the key created while you signed up to the app. Disabling this means that you can access the safe with any account as long as you have the password. Next is the dial of level of encryption. This is pretty self explanatory. After you have made the necessary changes, click on the \"confirm\" button. This would create the safe and add it to the list.

### Accessing a CRYPTILE safe -

 Interacting is also pretty straight forward. While on the main screen, click on the card that has the name of the safe you want to open. Upon clicking the card, you will see a prompt that asks for the safe's passwords. on the top of the card you will see two image buttons. Long press on them to see what they are used for. In the center of the card you will find two text boxes. depending on whether the safe uses one or two passwords, the \"Secondary Password\" text box one will be grayed out. Another thing you might notice is the lock icon at the end of the text boxes. these are used to disable the text boxes. This can be useful when the safe was crated by two people and each of them use separate password. Since you can lock the text boxes, the first password would not be accessible to the second person entering the password. To re-enable the text boxes, click on the clear button on the top of the card.

WARNING - Personal safes might not be accessible unless you have signed in using the account used to create the safe.

### Interacting with a CRYPTILE safe -

After entering the safe, you would see a list of items imported to the safe (empty if entering for the first time). Select item to open it in it's designated app. To add any file, click on the \"+\" button at the bottom of the screen. For options, open the options menu from the top app bar. In there, you can find options for adding files, safe settings (later), clear cache and send logs. Send logs can be used to send your safe's logs to us for analysis if some error occurs. can also long press an encrypted file for further options.

### Interacting with CRYPTILE safe Settings -

While inside a safe, select the option menu on the top app bar and select `Safe Settings` option to open a new prompt. Within the prompt, you'll find options to change the safe name, to export all files, delete the safe and change password. Exporting and changing password are processes that affect every file. So, these processes can take time proportional to the size of the safe contents. The reason changing password affects every file is because each password is used to generate a key. And so, if the password changes, the key it generates also changes. Hence, a change in password means re-encryption of every existing file in the safe.

### App settings -

To navigate to the app settings, be on the main screen. open the side option menu. Within settings you will find options to change your account's display name, password, the ability to delete your account with a warning statement attached. The other options are self explanator

### Remove All -

The remove all option is found in the side menu of the main screen. This option does not delete the safe. It just removes it from the app's database. This means that the safe is still located at the location you set for it during creation.

### Importing a CRYPTILE safe from storage -

This is also another simple task. In case you created a safe and removed it from the app's database, you can get it back into the app as long as you haven't deleted the safe files or modified the contents of the actual file. To Import the safe, click the `+` button on the main screen. There you can see an option to import the safe. Selecting that option you will be dropped off into your device's file explorer. Now, navigate to the directory inside the safe you want to import. Once inside the directory, you will see a file with the name `META_DATA.txt`l̥. Selecting that file would import the safe into the application. After that, you can interact with the safe as you normally would."
