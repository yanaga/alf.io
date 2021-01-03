/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.controller.payment.api;

import alfio.manager.PaymentManager;
import alfio.manager.PurchasableManager;
import alfio.manager.TicketReservationManager;
import alfio.manager.support.PaymentResult;
import alfio.model.Purchasable;
import alfio.model.TicketReservation;
import alfio.model.transaction.PaymentMethod;
import alfio.model.transaction.TransactionInitializationToken;
import alfio.repository.EventRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@AllArgsConstructor
public class PaymentApiController {

    private final PaymentManager paymentManager;
    private final TicketReservationManager ticketReservationManager;
    private final PurchasableManager purchasableManager;

    @PostMapping("/api/events/{eventName}/reservation/{reservationId}/payment/{method}/init")
    public ResponseEntity<TransactionInitializationToken> initTransaction(@PathVariable("eventName") String eventName,
                                                                          @PathVariable("reservationId") String reservationId,
                                                                          @PathVariable("method") String paymentMethodStr,
                                                                          @RequestParam MultiValueMap<String, String> allParams) {
        return initTransaction(Purchasable.PurchasableType.EVENT.getUrlComponent(), eventName, reservationId, paymentMethodStr, allParams);
    }

    @PostMapping("/api/{purchasableType}/{purchasableIdentifier}/reservation/{reservationId}/payment/{method}/init")
    public ResponseEntity<TransactionInitializationToken> initTransaction(@PathVariable("purchasableType") String purchasableType,
                                                                          @PathVariable("purchasableIdentifier") String purchasableIdentifier,
                                                                          @PathVariable("reservationId") String reservationId,
                                                                          @PathVariable("method") String paymentMethodStr,
                                                                          @RequestParam MultiValueMap<String, String> allParams) {

        var paymentMethod = PaymentMethod.safeParse(paymentMethodStr);
        if (paymentMethod == null) {
            return ResponseEntity.badRequest().build();
        }

        return getEventReservationPair(Purchasable.PurchasableType.from(purchasableType), purchasableIdentifier, reservationId)
            .flatMap(pair -> ticketReservationManager.initTransaction(pair.getLeft(), reservationId, paymentMethod, allParams))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Optional<Pair<? extends Purchasable, TicketReservation>> getEventReservationPair(Purchasable.PurchasableType type, String identifier, String reservationId) {
        return purchasableManager.findBy(type, identifier)
            .map(event -> Pair.of(event, ticketReservationManager.findById(reservationId)))
            .filter(pair -> pair.getRight().isPresent())
            .map(pair -> Pair.of(pair.getLeft(), pair.getRight().orElseThrow()));
    }

    @GetMapping("/api/events/{eventName}/reservation/{reservationId}/payment/{method}/status")
    public ResponseEntity<PaymentResult> getTransactionStatus(@PathVariable("eventName") String eventName,
                                                              @PathVariable("reservationId") String reservationId,
                                                              @PathVariable("method") String paymentMethodStr) {
        return getTransactionStatus(Purchasable.PurchasableType.EVENT.getUrlComponent(), eventName, reservationId, paymentMethodStr);
    }

    @GetMapping("/api/{purchasableType}/{purchasableIdentifier}/reservation/{reservationId}/payment/{method}/status")
    public ResponseEntity<PaymentResult> getTransactionStatus(@PathVariable("purchasableType") String purchasableType,
                                                              @PathVariable("purchasableIdentifier") String purchasableIdentifier,
                                                              @PathVariable("reservationId") String reservationId,
                                                              @PathVariable("method") String paymentMethodStr) {
        var paymentMethod = PaymentMethod.safeParse(paymentMethodStr);
        if (paymentMethod == null) {
            return ResponseEntity.badRequest().build();
        }

        return getEventReservationPair(Purchasable.PurchasableType.from(purchasableType), purchasableIdentifier, reservationId)
            .flatMap(pair -> paymentManager.getTransactionStatus(pair.getRight(), paymentMethod))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v2/public/{purchasableType}/{purchasableIdentifier}/reservation/{reservationId}/transaction/force-check")
    public ResponseEntity<PaymentResult> forceCheckStatus(@PathVariable("purchasableType") String purchasableType,
                                                          @PathVariable("purchasableIdentifier") String purchasableIdentifier,
                                                          @PathVariable("reservationId") String reservationId) {
        return ResponseEntity.of(getEventReservationPair(Purchasable.PurchasableType.from(purchasableType), purchasableIdentifier, reservationId)
            .flatMap(pair -> ticketReservationManager.forceTransactionCheck(pair.getLeft(), pair.getRight())));
    }
}
