// Copyright 2016 The Nomulus Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.flows.contact;

import static google.registry.flows.FlowUtils.validateClientIsLoggedIn;
import static google.registry.flows.ResourceFlowUtils.loadAndVerifyExistence;
import static google.registry.flows.ResourceFlowUtils.verifyOptionalAuthInfoForResource;
import static google.registry.model.EppResourceUtils.cloneResourceWithLinkedStatus;

import com.google.common.base.Optional;
import google.registry.flows.EppException;
import google.registry.flows.ExtensionManager;
import google.registry.flows.Flow;
import google.registry.flows.FlowModule.ClientId;
import google.registry.flows.FlowModule.TargetId;
import google.registry.model.contact.ContactResource;
import google.registry.model.eppcommon.AuthInfo;
import google.registry.model.eppoutput.EppResponse;
import google.registry.util.Clock;
import javax.inject.Inject;
import org.joda.time.DateTime;

/**
 * An EPP flow that returns information about a contact.
 *
 * <p>The response includes the contact's postal info, phone numbers, emails, the authInfo which can
 * be used to request a transfer and the details of the contact's most recent transfer if it has
 * ever been transferred. Any registrar can see any contact's information, but the authInfo is only
 * visible to the registrar that owns the contact or to a registrar that already supplied it.
 *
 * @error {@link google.registry.flows.ResourceFlowUtils.ResourceDoesNotExistException}
 */
public final class ContactInfoFlow implements Flow {

  @Inject ExtensionManager extensionManager;
  @Inject Clock clock;
  @Inject @ClientId String clientId;
  @Inject @TargetId String targetId;
  @Inject Optional<AuthInfo> authInfo;
  @Inject EppResponse.Builder responseBuilder;
  @Inject ContactInfoFlow() {}

  @Override
  public final EppResponse run() throws EppException {
    DateTime now = clock.nowUtc();
    extensionManager.validate();  // There are no legal extensions for this flow.
    validateClientIsLoggedIn(clientId);
    ContactResource contact = loadAndVerifyExistence(ContactResource.class, targetId, now);
    verifyOptionalAuthInfoForResource(authInfo, contact);
    if (!clientId.equals(contact.getCurrentSponsorClientId()) && !authInfo.isPresent()) {
      contact = contact.asBuilder().setAuthInfo(null).build();
    }
    return responseBuilder.setResData(cloneResourceWithLinkedStatus(contact, now)).build();
  }
}
