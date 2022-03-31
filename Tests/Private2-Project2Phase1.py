from http import HTTPStatus
from threading import Thread
import requests

# Check if only one order is assigned and other
# invalid requests whose qty is not 
# available with restaurant gets 
# are not delivered and balance of user is added correctly after deduction
# in later request. 

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082

def test():

	result = {}

	# Reinitialize Restaurant service
	http_response = requests.post("http://localhost:8080/reInitialize")

	# Reinitialize Delivery service
	http_response = requests.post("http://localhost:8081/reInitialize")

	# Reinitialize Wallet service
	http_response = requests.post("http://localhost:8082/reInitialize")

	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 2, "qty": 8})

	if(http_response.status_code != HTTPStatus.CREATED):
		return 'Fail'

	order_id = http_response.json().get("orderId")

	if(order_id != 1000):
		return 'Fail'

	http_response = requests.get(
		f"http://localhost:8081/order/{order_id}")

	if(http_response.status_code != HTTPStatus.OK):
		return 'Fail'

	if(http_response.json().get("status") != 'delivered'):
		return 'Fail'

	http_response = requests.get(
		f"http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		return 'Fail'

	before_balance = http_response.json().get("balance")

	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 2, "qty": 20})

	if(http_response.status_code != HTTPStatus.CREATED):
		return 'Fail'

	order_id = http_response.json().get("orderId")

	if(order_id != 1001):
		return 'Fail'

	http_response = requests.get(
		f"http://localhost:8081/order/{order_id}")

	if(http_response.status_code != HTTPStatus.OK):
		return 'Fail'

	if(http_response.json().get("status") != 'unassigned'):	
		return 'Fail'

	http_response = requests.get(
		f"http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		return 'Fail'

	after_balance = http_response.json().get("balance")

	if(before_balance != after_balance):
		return 'Fail'

	return 'Pass'

	
if __name__ == "__main__":

	print(test())
