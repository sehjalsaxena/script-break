# Demo QR images

Made with ZXing (`docs/demo_qr`). They use demo UPI IDs only (`@demoupi`), so no real money. Each one decodes back to its text
(checked when it was made).

| Image | Inside the QR | What the app does |
|---|---|---|
| `1_scam_refund_qr.png` | `upi://pay?pa=ramesh.k@demoupi&pn=Bank%20Refund%20Desk&am=20000&cu=INR&tn=Refund%20verification` | Payee Ramesh K. (new). Warns that the QR's name "Bank Refund Desk" is not the banking name. On a call: Pause |
| `2_tv_gupta_electronics_qr.png` | `upi://pay?pa=guptaelectronics@demoupi&pn=Gupta%20Electronics&am=90000&cu=INR&tn=TV` | Genuine shop. No call: amber line only, then the PIN |
| `3_rent_suresh_qr.png` | `upi://pay?pa=suresh.verma@demoupi&pn=Suresh%20Verma&am=18000&cu=INR&tn=Rent` | Usual landlord: nothing extra, straight to the PIN |

**Getting them onto the phone:** send them to yourself (WhatsApp, Telegram or email) and save them to the gallery, or
copy them over USB into `Pictures`. Then in DemoPay: Home → QR icon → pick the image.

A judge can also show any of them on a laptop screen: take a photo with the phone's camera, then pick that photo.
