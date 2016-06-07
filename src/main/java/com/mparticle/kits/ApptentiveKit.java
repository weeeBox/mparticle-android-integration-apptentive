package com.mparticle.kits;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;

import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.ApptentiveInternal;
import com.apptentive.android.sdk.lifecycle.ApptentiveActivityLifecycleCallbacks;
import com.apptentive.android.sdk.model.CommerceExtendedData;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ApptentiveKit extends KitIntegration implements KitIntegration.EventListener, KitIntegration.CommerceListener, KitIntegration.AttributeListener, KitIntegration.ActivityListener {
	private static final String API_KEY = "appKey";
	private ApptentiveActivityLifecycleCallbacks callbacks;

	@Override
	public String getName() {
		return "Apptentive";
	}

	@Override
	protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
		if (callbacks == null) {
			callbacks = new ApptentiveActivityLifecycleCallbacks();
		}
		/* Note mParticle will delegate lifecycle management to the above callbacks. No need to
		 * register ApptentiveActivityLifecycleCallbacks through Apptentive. But do need to initialize Apptentive
         *
         */
		ApptentiveInternal.createInstance(context.getApplicationContext(), settings.get(API_KEY));
		return null;
	}

	@Override
	public List<ReportingMessage> setOptOut(boolean optedOut) {
		return null;
	}

	@Override
	public void setUserIdentity(MParticle.IdentityType identityType, String id) {
		if (identityType.equals(MParticle.IdentityType.Email)) {
			Apptentive.setPersonEmail(id);
		} else if (identityType.equals(MParticle.IdentityType.CustomerId)) {
			if (TextUtils.isEmpty(Apptentive.getPersonName())) {
				// Use id as customer name iff no full name is set yet.
				Apptentive.setPersonName(id);
			}
		}
	}

	@Override
	public void setUserAttribute(String attributeKey, String attributeValue) {
		String firstName = "";
		String lastName = "";

		if (attributeKey.equalsIgnoreCase(MParticle.UserAttributes.FIRSTNAME)) {
			firstName = attributeValue;
		} else if (attributeKey.equalsIgnoreCase(MParticle.UserAttributes.LASTNAME)) {
			lastName = attributeValue;
		} else {
			Apptentive.addCustomPersonData(attributeKey, attributeValue);
		}

		String fullName;
		if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName)) {
			fullName = firstName + " " + lastName;
		} else {
			fullName = firstName + lastName;
		}
		Apptentive.setPersonName(fullName.trim());
	}

	@Override
	public void setUserAttributeList(String key, List<String> list) {

	}

	@Override
	public boolean supportsAttributeLists() {
		return false;
	}

	@Override
	public void setAllUserAttributes(Map<String, String> attributes, Map<String, List<String>> attributeLists) {
		String firstName = "";
		String lastName = "";
		for (Map.Entry<String, String> entry : attributes.entrySet()){
			if (entry.getKey().equalsIgnoreCase(MParticle.UserAttributes.FIRSTNAME)) {
				firstName = entry.getValue();
			} else if (entry.getKey().equalsIgnoreCase(MParticle.UserAttributes.LASTNAME)) {
				lastName = entry.getValue();
			} else {
				Apptentive.addCustomPersonData(entry.getKey(), entry.getValue());
			}
		}
		String fullName;
		if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName)) {
			fullName = firstName + " " + lastName;
		} else {
			fullName = firstName + lastName;
		}
		Apptentive.setPersonName(fullName.trim());
	}

	@Override
	public void removeUserAttribute(String key) {
		Apptentive.removeCustomPersonData(key);
	}


	@Override
	public void removeUserIdentity(MParticle.IdentityType identityType) {

	}

	@Override
	public List<ReportingMessage> logout() {
		return null;
	}

	@Override
	public List<ReportingMessage> leaveBreadcrumb(String breadcrumb) {
		return null;
	}

	@Override
	public List<ReportingMessage> logError(String message, Map<String, String> errorAttributes) {
		return null;
	}

	@Override
	public List<ReportingMessage> logException(Exception exception, Map<String, String> exceptionAttributes, String message) {
		return null;
	}

	@Override
	public List<ReportingMessage> logEvent(MPEvent event) {
		Map<String, String> customData = event.getInfo();
		if (customData != null) {
			Apptentive.engage(getContext(), event.getEventName(), Collections.<String, Object>unmodifiableMap(customData));
		} else {
			Apptentive.engage(getContext(), event.getEventName());
		}
		List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
		messageList.add(ReportingMessage.fromEvent(this, event));
		return messageList;
	}

	@Override
	public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) {
		return null;
	}

	@Override
	public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo) {
		return null;
	}

	@Override
	public List<ReportingMessage> logEvent(CommerceEvent event) {
		if (!TextUtils.isEmpty(event.getProductAction())) {
			try {
				Map<String, String> eventActionAttributes = new HashMap<String, String>();
				CommerceEventUtil.extractActionAttributes(event, eventActionAttributes);

				CommerceExtendedData apptentiveCommerceData = null;

				TransactionAttributes transactionAttributes = event.getTransactionAttributes();
				if (transactionAttributes != null) {
					apptentiveCommerceData = new CommerceExtendedData();

					String transaction_id = transactionAttributes.getId();
					if (!TextUtils.isEmpty(transaction_id)) {
						apptentiveCommerceData.setId(transaction_id);
					}
					Double transRevenue = transactionAttributes.getRevenue();
					if (transRevenue != null) {
						apptentiveCommerceData.setRevenue(transRevenue);
					}
					Double transShipping = transactionAttributes.getShipping();
					if (transShipping != null) {
						apptentiveCommerceData.setShipping(transShipping);
					}
					Double transTax = transactionAttributes.getTax();
					if (transTax != null) {
						apptentiveCommerceData.setTax(transTax);
					}
					String transAffiliation = transactionAttributes.getAffiliation();
					if (!TextUtils.isEmpty(transAffiliation)) {
						apptentiveCommerceData.setAffiliation(transAffiliation);
					}
					String transCurrency = eventActionAttributes.get(Constants.Commerce.ATT_ACTION_CURRENCY_CODE);
					if (TextUtils.isEmpty(transCurrency)) {
						transCurrency = Constants.Commerce.DEFAULT_CURRENCY_CODE;
					}
					apptentiveCommerceData.setCurrency(transCurrency);

					// Add each item
					List<Product> productList = event.getProducts();
					if (productList != null) {
						for (Product product : productList) {
							CommerceExtendedData.Item item = new CommerceExtendedData.Item();
							item.setId(product.getSku());
							item.setName(product.getName());
							item.setCategory(product.getCategory());
							item.setPrice(product.getUnitPrice());
							item.setQuantity(product.getQuantity());
							item.setCurrency(transCurrency);
							apptentiveCommerceData.addItem(item);
						}
					}
				}


				if (apptentiveCommerceData != null) {
					Map<String, String> customData = event.getCustomAttributes();
					Apptentive.engage(getContext(),
							String.format("eCommerce - %s", event.getProductAction()),
							customData == null ? null : Collections.<String, Object>unmodifiableMap(customData),
							apptentiveCommerceData);
					List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
					messages.add(ReportingMessage.fromEvent(this, event));
					return messages;
				}
			}catch (JSONException jse) {

			}

		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityCreated(Activity activity, Bundle savedInstanceState) {
		if (callbacks != null) {
			callbacks.onActivityCreated(activity, null);
		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityResumed(Activity activity) {
		if (callbacks != null) {
			callbacks.onActivityResumed(activity);
		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityPaused(Activity activity) {
		if (callbacks != null) {
			callbacks.onActivityPaused(activity);
		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityStopped(Activity activity) {
		if (callbacks != null) {
			callbacks.onActivityStopped(activity);
		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivitySaveInstanceState(Activity activity, Bundle outState) {
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityStarted(Activity activity) {
		if (callbacks != null) {
			callbacks.onActivityStarted(activity);
		}
		return null;
	}

	@Override
	public List<ReportingMessage> onActivityDestroyed(Activity activity) {
		return null;
	}
}