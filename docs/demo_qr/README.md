# Demo QR images

Made with ZXing (`docs/demo_qr`). They use demo UPI IDs only (`@demoupi`), so no real money. Each one decodes back to its text
(checked when it was made).

| Image | Inside the QR | What the app does |
|---|---|---|
| `1_scam_refund_qr.png` | `upi://pay?pa=ramesh.k@demoupi&pn=Bank%20Refund%20Desk&am=20000&cu=INR&tn=Refund%20verification` | Payee Ramesh K. (new). Warns that the QR's name "Bank Refund Desk" is not the banking name. On a call: Pause |
| `2_tv_gupta_electronics_qr.png` | `upi://pay?pa=guptaelectronics@demoupi&pn=Gupta%20Electronics&am=90000&cu=INR&tn=TV` | Genuine shop. No call: amber line only, then the PIN |
| `3_rent_suresh_qr.png` | `upi://pay?pa=suresh.verma@demoupi&pn=Suresh%20Verma&am=18000&cu=INR&tn=Rent` | Usual landlord: nothing extra, straight to the PIN |

**Camera:** show the image on a laptop or another phone, then in DemoPay: Home → QR icon → point the camera at it.
Judge mode: Sandbox → **Fill from a QR code**.

**From the gallery:** send the images to yourself (WhatsApp, Telegram or email) and save them, or copy them over USB into
`Pictures`. Then on the scanner screen tap **Pick a QR image instead**.
