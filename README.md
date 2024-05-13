# CRAB MOBILE

Đây là ứng dụng mobile hỗ trợ cho việc đặt xe của khách hàng và nhận cuốc của tài xế.

## Folder structure

```
.
├── app
│   ├── build
│   ├── src
│   │   ├── androidTest
│   │   ├── main
│   │   │   ├── java.tkpm.com.crab
│   │   │   │   ├── activity
│   │   │   │   │   ├── authentication
│   │   │   │   │   │   ├── phone
│   │   │   │   │   │   │   ├── PhoneLoginActivity.kt
│   │   │   │   │   │   │   └── PhoneVerificationActivity.kt
│   │   │   │   │   │   └── LoginActivity.kt
│   │   │   │   │   ├── customer
│   │   │   │   │   │   ├── ChoosePaymentActivity.kt
│   │   │   │   │   │   ├── CustomerMapsActivity.kt
│   │   │   │   │   │   ├── CustomerRatingActivity.kt
│   │   │   │   │   │   ├── NewPaymentMethodActivity.kt
│   │   │   │   │   │   └── PaymentMethodActivity.kt
│   │   │   │   │   ├── driver
│   │   │   │   │   │   ├── ChangeVehicleInfo.kt
│   │   │   │   │   │   ├── CompleteOrderActivity.kt
│   │   │   │   │   │   ├── DriverIncomeActivity.kt
│   │   │   │   │   │   ├── DriverMapActivity.kt
│   │   │   │   │   │   ├── DriverRatingActivity.kt
│   │   │   │   │   │   ├── DriverTopupPayoutActivity.kt
│   │   │   │   │   │   └── DriverTransactionsHistory.kt
│   │   │   │   │   ├── ChangeInfoActivity.kt
│   │   │   │   │   ├── HistoryActivity.kt
│   │   │   │   │   ├── HistoryDetailActivity.kt
│   │   │   │   │   ├── SuggestionActivity.kt
│   │   │   │   │   └── UpdateInfoActivity.kt
│   │   │   │   ├── adapter
│   │   │   │   │   ├── ChatAdpater.kt
│   │   │   │   │   ├── CustomWindowInfo.kt
│   │   │   │   │   ├── MapPredictionAdapter.kt
│   │   │   │   │   ├── SuggestionAdapter.kt
│   │   │   │   │   └── TypeVehicleAdapter.kt
│   │   │   │   ├── api
│   │   │   │   │   ├── APICallback.kt
│   │   │   │   │   ├── APIInterface.kt
│   │   │   │   │   ├── APIService.kt
│   │   │   │   │   └── RetrofitInstance.kt
│   │   │   │   ├── constant
│   │   │   │   │   └── NOTIFICATION.kt
│   │   │   │   ├── credential_service
│   │   │   │   │   └── CredentialSerivce.kt
│   │   │   │   ├── dialog
│   │   │   │   │   ├── LoadingDialog.kt
│   │   │   │   │   └── TimeOutDialog.kt
│   │   │   │   ├── notification
│   │   │   │   │   └── CustomerNotification.kt
│   │   │   │   ├── objects
│   │   │   │   │   ├── Account.kt
│   │   │   │   │   ├── BaseResponse.kt
│   │   │   │   │   ├── Booking.kt
│   │   │   │   │   ├── BookingRequest.kt
│   │   │   │   │   ├── Bucket.kt
│   │   │   │   │   ├── Income.kt
│   │   │   │   │   ├── Message.kt
│   │   │   │   │   ├── NotificationToken.kt
│   │   │   │   │   ├── PaymentMethod.kt
│   │   │   │   │   ├── Rating.kt
│   │   │   │   │   ├── Suggestion.kt
│   │   │   │   │   ├── User.kt
│   │   │   │   │   ├── Vehicle.kt
│   │   │   │   │   └── VehicleTypePrice.kt
│   │   │   │   ├── utils
│   │   │   │   │   ├── CreditCardNumberFormattingTextWatcher.kt
│   │   │   │   │   ├── DirectionRequest.kt
│   │   │   │   │   └── PriceDisplay.kt
│   │   │   │   ├── CrabApplication.kt
│   │   │   │   ├── CustomeAutocompleteTextView.kt
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res
│   │   │   │   ├── anim
│   │   │   │   │   ├── bottom_animation.xml
│   │   │   │   │   └── logo_animation.xml
│   │   │   │   ├── drawable
│   │   │   │   │   ├── avd_done.xml
│   │   │   │   │   ├── bg_otp_item.xml
│   │   │   │   │   ├── bg_search_box.xml
│   │   │   │   │   ├── bottom_bg_login.xml
│   │   │   │   │   ├── bottom_layout.xml
│   │   │   │   │   ├── coming_driver.gif
│   │   │   │   │   ├── flag_vn.png
│   │   │   │   │   ├── gif_search.gif
│   │   │   │   │   ├── grab_splash.png
│   │   │   │   │   ├── ic_bike.png
│   │   │   │   │   ├── ic_card.xml
│   │   │   │   │   ├── ic_car.png
│   │   │   │   │   ├── ic_cash.xml
│   │   │   │   │   ├── ic_close.xml
│   │   │   │   │   ├── ic_complete_check.xml
│   │   │   │   │   ├── ic_current_location.xml
│   │   │   │   │   ├── ic_driver.png
│   │   │   │   │   ├── ic_grab.xml
│   │   │   │   │   ├── ic_great.png
│   │   │   │   │   ├── ic_green_circle.xml
│   │   │   │   │   ├── ic_launcher_background.xml
│   │   │   │   │   ├── ic_launcher_foreground.xml
│   │   │   │   │   ├── ic_like.png
│   │   │   │   │   ├── ic_marker.xml
│   │   │   │   │   ├── ic_menu.xml
│   │   │   │   │   ├── ic_next_arrow.xml
│   │   │   │   │   ├── ic_power_google.xml
│   │   │   │   │   ├── ic_power.xml
│   │   │   │   │   ├── ic_rating.xml
│   │   │   │   │   ├── ic_search.png
│   │   │   │   │   ├── ic_send.xml
│   │   │   │   │   ├── ic_star.xml
│   │   │   │   │   ├── ic_time.xml
│   │   │   │   │   ├── ic_user.png
│   │   │   │   │   ├── ic_visa.xml
│   │   │   │   │   ├── ic_wait.gif
│   │   │   │   │   ├── lock_timeout.xml
│   │   │   │   │   ├── login_bg.png
│   │   │   │   │   ├── no_img.jpg
│   │   │   │   │   ├── rounded_corner.xml
│   │   │   │   │   └── window_info_background.xml
│   │   │   │   ├── font
│   │   │   │   │   └── grab_font.otf
│   │   │   │   ├── layout
│   │   │   │   │   ├── activity_change_vehicle_info.xml
│   │   │   │   │   ├── activity_complete_order.xml
│   │   │   │   │   ├── activity_driver_income.xml
│   │   │   │   │   ├── activity_driver_maps.xml
│   │   │   │   │   ├── activity_driver_topup_payout.xml
│   │   │   │   │   ├── activity_driver_transactions_history.xml
│   │   │   │   │   ├── activity_history_detail.xml
│   │   │   │   │   ├── activity_history.xml
│   │   │   │   │   ├── activity_login.xml
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_maps.xml
│   │   │   │   │   ├── activity_new_payment_method.xml
│   │   │   │   │   ├── activity_payment_method.xml
│   │   │   │   │   ├── activity_phone_login.xml
│   │   │   │   │   ├── activity_phone_verification.xml
│   │   │   │   │   ├── activity_rating.xml
│   │   │   │   │   ├── activity_suggestion.xml
│   │   │   │   │   ├── activity_update_info.xml
│   │   │   │   │   ├── bottom_choose_location.xml
│   │   │   │   │   ├── bottom_customer_waiting.xml
│   │   │   │   │   ├── bottom_driver_arrived.xml
│   │   │   │   │   ├── bottom_driver_coming.xml
│   │   │   │   │   ├── bottom_finish_trip.xml
│   │   │   │   │   ├── bottom_function_driver.xml
│   │   │   │   │   ├── bottom_type_vehicle.xml
│   │   │   │   │   ├── chat_item.xml
│   │   │   │   │   ├── custom_windowinfo.xml
│   │   │   │   │   ├── item_history_layout.xml
│   │   │   │   │   ├── item_map_prediction_row_layout.xml
│   │   │   │   │   ├── item_payment_method_layout.xml
│   │   │   │   │   ├── item_vehicle_type_row_layout.xml
│   │   │   │   │   ├── left_driver_menu.xml
│   │   │   │   │   ├── left_user_menu.xml
│   │   │   │   │   ├── loading_dialog.xml
│   │   │   │   │   ├── suggestion_item.xml
│   │   │   │   │   ├── timeout_dialog.xml
│   │   │   │   │   └── transaction_item_layout.xml
│   │   │   │   ├── mipmap-anydpi-v26
│   │   │   │   │   ├── ic_launcher_round.xml
│   │   │   │   │   └── ic_launcher.xml
│   │   │   │   ├── mipmap-hdpi
│   │   │   │   │   ├── ic_launcher_round.webp
│   │   │   │   │   └── ic_launcher.webp
│   │   │   │   ├── mipmap-mdpi
│   │   │   │   │   ├── ic_launcher_round.webp
│   │   │   │   │   └── ic_launcher.webp
│   │   │   │   ├── mipmap-xhdpi
│   │   │   │   │   ├── ic_launcher_round.webp
│   │   │   │   │   └── ic_launcher.webp
│   │   │   │   ├── mipmap-xxhdpi
│   │   │   │   │   ├── ic_launcher_round.webp
│   │   │   │   │   └── ic_launcher.webp
│   │   │   │   ├── mipmap-xxxhdpi
│   │   │   │   │   ├── ic_launcher_round.webp
│   │   │   │   │   └── ic_launcher.webp
│   │   │   │   ├── values
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── styles.xml
│   │   │   │   │   ├── theme_overlays.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── values-night
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── theme_overlays.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── xml
│   │   │   │       ├── backup_rules.xml
│   │   │   │       ├── data_extraction_rules.xml
│   │   │   │       └── network_security_config.xml
│   │   │   └── AndroidManifest.xml
│   │   ├─── test
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── proguard-rules.pro
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.defaults.properties
├── local.properties
├── secrets.properties
└── settings.gradle.kts

```

## Yêu cầu môi trường

-   Android Studio Jellyfish | 2023.3.1
-   Android 13, minSdkVersion 28, targetSdkVersion 34, compileSdkVersion 34
-   **secrets.properties**:

```
MAPS_API_KEY=AAA
BASE_URL=http://server_address:3000/api/
BASE_URL_WS=ws://server_address:8080
```

AAA là Google Map API key. \
server_address là địa chỉ IP đến server (có thể dùng Nginx cùng với domain để thay thế cho IP và port).

## Build app:
Trên thanh công cụ tìm đến **Build -> Build App Bundle(s)/APK(s) -> Build APK(s)** để build app.