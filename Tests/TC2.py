from http import HTTPStatus
import requests
import time


# Initially tries to place an order with quantity higher than available
# First order is rejected
# Customer's balance consistency is checked
# The item is now refilled with the desired quantity
# Now, the order should be successful


# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def test():

	test_result = 'Pass'

	# Reinitialize Restaurant service
	http_response = requests.post("http://localhost:8080/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail1'

	# Reinitialize Delivery service
	http_response = requests.post("http://localhost:8081/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail2'

	# Reinitialize Wallet service
	http_response = requests.post("http://localhost:8082/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail3'


	#Check customer 301's balance before the invalid request
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail4"

	res_body = http_response.json()
	initial_balance = res_body.get("balance")


	#Place an order with high quantity
	http_response = requests.post("http://localhost:8081/requestOrder", json={"custId":301, "restId":102, "itemId":1, "qty":20})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail5"
	
	time.sleep(5)
	#Check customer 301's balance after the invalid request
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail6"

	res_body = http_response.json()
	after_balance = res_body.get("balance")	


	if initial_balance != after_balance:
		test_result = "Fail7"


	#Add items to the inventory
	http_response = requests.post("http://localhost:8080/refillItem", json={"restId":102, "itemId":1, "qty":10})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail8"


	#Check customer 301's balance before the valid request
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail9"

	res_body = http_response.json()
	initial_balance = res_body.get("balance")
	
	#Now try to place the order with the same quantity again. It should succeed this time
	http_response = requests.post("http://localhost:8081/requestOrder", json={"custId":301, "restId":102, "itemId":1, "qty":20})

	time.sleep(5)
	res_body = http_response.json()
	order_id = -1
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail10"
	else:
		order_id = res_body.get("orderId")

	if (order_id == -1):
		test_result = 'Fail11'

	#Check customer 301's balance after the valid request
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail12"

	res_body = http_response.json()
	after_balance = res_body.get("balance")	

	if after_balance >= initial_balance:
		test_result = "Fail13"


	return test_result


if __name__ == "__main__":
	test_result = test()
	print(test_result)
