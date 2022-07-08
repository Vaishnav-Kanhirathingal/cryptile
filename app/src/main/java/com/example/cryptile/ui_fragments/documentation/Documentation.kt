package com.example.cryptile.ui_fragments.documentation

data class Documentation(
    val title: String,
    val description: String
)

object Docs {
    val list = listOf<Documentation>(
        Documentation(
            title = "Sign Up",
            description = "You can either sign up using the Google account Sign up process or your " +
                    "email address. Signing up to the app creates your account name and a secret key. " +
                    "This key's use case will be discussed later on during the creation of a safe "
        ),
        Documentation(
            title = "Creating a CRYPTILE safe",
            description = "Creating a Safe is quite easy. Once on the main screen click the " +
                    "circular \"+\" button. This will navigate you to the \"create safe\" screen. " +
                    "In there, all you have to do is provide the application with the name of " +
                    "the safe, the password you want to give it. Now, here, you can use " +
                    "an additional password if you so require. Next you can also provide the safe " +
                    "with its own separate location. the default location is provided and changing " +
                    "that is optional. You can change the location using the image button on the " +
                    "right of the card. Select the folder where you want to create the safe. " +
                    "After selection, the path value should be updated in the app. Next up is the " +
                    "\"Personal Access Only?\" option. Enabling this option means only you can " +
                    "access the safe. When this option is enabled, the safe adds another layer " +
                    "of encryption using the key created while you signed up to the app. " +
                    "Disabling this means that you can access the safe with any account as long " +
                    "as you have the password. Next is the dial of level of encryption. This is " +
                    "pretty self explanatory. After you have made the necessary changes, click " +
                    "on the \"confirm\" button. This would create the safe and add it to the list."
        ),
        Documentation(
            title = "Accessing a CRYPTILE safe",
            description = "Interacting is also pretty straight forward. While on the main " +
                    "screen, click on the card that has the name of the safe you want to " +
                    "open. Upon clicking the card, you will see a prompt that asks for the " +
                    "safe's passwords. on the top of the card you will see two image buttons. " +
                    "Long press on them to see what they are used for. In the center of the card " +
                    "you will find two text boxes. depending on whether the safe uses one or two " +
                    "passwords, the \"Secondary Password\" text box one will be grayed out. " +
                    "Another thing you might notice is the lock icon at the end of the text " +
                    "boxes. these are used to disable the text boxes. This can be useful when " +
                    "the safe was crated by two people and each of them use separate password. " +
                    "Since you can lock the text boxes, the first password would not be " +
                    "accessible to the second person entering the password. To re-enable the " +
                    "text boxes, click on the clear button on the top of the card. " +
                    "\nWARNING - Personal safes might not be accessible unless you have signed " +
                    "in using the account used to create the safe."
        ),
        Documentation(
            title = "Interacting with a CRYPTILE safe",
            description = "After entering the safe, you would see a list of items imported to " +
                    "the safe (empty if entering for the first time). To add any file, click " +
                    "on the \"+\" button at the bottom of the screen."// TODO: finish
        )
    )
}