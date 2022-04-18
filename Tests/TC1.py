from http import HTTPStatus
import requests
import time

# Tries to place an order which should succeed
# Agent status consistency check
# Customer's balance updation check
# Order status check


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

	#Check customer 301's balance before placing the order
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail4"

	res_body = http_response.json()
	initial_balance = res_body.get("balance")


	#PTry to place a valid order
	http_response = requests.post("http://localhost:8081/requestOrder", json={"custId":301, "restId":101, "itemId":1, "qty":4})

	res_body = http_response.json()
	order_id = -1
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail5"
	else:
		order_id = res_body.get("orderId")

	if (order_id == -1):
		test_result = 'Fail6'


	#Sign in agent 201
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId":201})
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail7"

	time.sleep(5)
	#Check the order status. It should be assigned to agent 201
	http_response = requests.get("http://localhost:8081/order/"+str(order_id))

	res_body = http_response.json()
	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail8"
	else:
		agent_id = res_body.get("agentId")
		status = res_body.get("status")

		if(status != "assigned" or agent_id != 201):
			test_result = "Fail9"


	#Check the agent 201's status. It should be unavailable
	http_response = requests.get("http://localhost:8081/agent/201")

	res_body = http_response.json()
	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail10"
	else:
		status = res_body.get("status")

		if(status != "unavailable"):
			test_result = "Fail11"


	#Check customer 301's balance after the order
	http_response = requests.get("http://localhost:8082/balance/301")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail12"

	res_body = http_response.json()
	after_balance = res_body.get("balance")	


	if after_balance >= initial_balance:
		test_result = "Fail13"


	#Deliver the order successfully
	http_response = requests.post("http://localhost:8081/orderDelivered", json={"orderId":str(order_id)})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail14"

	time.sleep(5)
	#Now the order status should be delivered
	http_response = requests.get("http://localhost:8081/order/"+str(order_id))

	res_body = http_response.json()
	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail15"
	else:
		status = res_body.get("status")

		if(status != "delivered"):
			test_result = "Fail16"

	time.sleep(5)
	#The agent 201 should now be in available state.
	http_response = requests.get("http://localhost:8081/agent/201")

	res_body = http_response.json()
	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail17"
	else:
		status = res_body.get("status")

		if(status != "available"):
			test_result = "Fail18"

	return test_result


if __name__ == "__main__":
	test_result = test()
	print(test_result)
