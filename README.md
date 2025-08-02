<div align="center">

<img width="" src="metadata/en-US/images/icon.png"  width=160 height=160  align="center">

# Centsation

Centsation is the easy-to-use savings tracker that helps you reach your financial goals! Whether you're saving for a vacation, a new gadget, or an emergency fund, Centsation makes it simple and straightforward.

</div>

## Features

* **Free and Open Source:** Enjoy complete transparency and community-driven development.
* **Modern Design:** Experience a beautiful and intuitive interface with Material 3 and dynamic color support.
* **Dark Mode:** Save battery and reduce eye strain with a sleek dark theme.
* **Multi-Currency Support:** Track your savings in a currency of your choice.
* **Archived Savings:** Keep your active savings organized by archiving completed or paused goals.
* **Data Management:** Easily export and import your data using JSON files.
* **Transaction History:** Review a detailed log of your savings activities.
* **Optional Deadlines:** Set and track progress towards your goals with optional deadlines.
* **Flexible Sorting:** Organize your savings by name, current amount, goal amount, or deadline.

## Screenshots

<div align="center">
	<div>
		<img src="metadata/en-US/images/phoneScreenshots/Screenshot 1.jpg" width="30%" />
    <img src="metadata/en-US/images/phoneScreenshots/Screenshot 2.jpg" width="30%" />
    <img src="metadata/en-US/images/phoneScreenshots/Screenshot 3.jpg" width="30%" />
    <img src="metadata/en-US/images/phoneScreenshots/Screenshot 4.jpg" width="30%" />
    <img src="metadata/en-US/images/phoneScreenshots/Screenshot 5.jpg" width="30%" />
    <img src="metadata/en-US/images/phoneScreenshots/Screenshot 6.jpg" width="30%" />
	</div>
</div>

## Downloads

Centsation is also available on IzzyOnDroid.

[<img height=80 alt="Get it on IzzyOnDroid"
src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
/>](https://apt.izzysoft.de/fdroid/index/apk/com.eipna.centsation)

## Verification

APK releases on GitHub are signed using my key. They can
be verified using
[apksigner](https://developer.android.com/studio/command-line/apksigner.html#options-verify):

```
apksigner verify --print-certs --verbose centsation.apk
```

The output should look like:

```
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
```

The certificate fingerprints should correspond to the ones listed below:

```
CN=Vrixzandro Eliponga
O=OSSentials
OU=Hobbyist Developer
L=Lian
ST=Batangas
C=PH
Certificate fingerprints:
   MD5:  a8a82d68a60fe6ecf45eff4550b94d6f
   SHA1: af8be376426c6725fc3bdb287abeb268bf94b768
   SHA256: 561f3fec72e1f9878c2749d079f8b2175d02131c842955714e63365da5301baa
```

**Warning:** Please be aware that versions 1.3 and below of this application were released without digital signatures. For the best security and to ensure you are using a genuine version of the application, I strongly recommend updating to the latest version (1.4 or higher) as soon as possible.

## License

This project is licensed under the GNU General Public License v3.0. See the
[LICENSE](LICENSE) file for details.
