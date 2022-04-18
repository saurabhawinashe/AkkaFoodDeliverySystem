from http import HTTPStatus
from threading import Thread
import requests
import time
 
 
# Agent should not move from unavailable state to signed-out state on receiving an agentSignOut request from client.
# After the order is delivered, agentSignOut should move the Agent to signed-out state

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

	# Sign In One agent
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 201})

	result["1"] = http_response


def t2(result):  # Second concurrent request

	# Customer 302 requests an order of item 1, quantity 3 from restaurant 101
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 202})

	result["2"] = http_response

def t3(result):  # First concurrent request

	# Sign In One agent
	http_response = requests.post(
		"http://localhost:8081/agentSignOut", json={"agentId": 201})

	result["3"] = http_response


def t4(result):  # Second concurrent request

	# Customer 302 requests an order of item 1, quantity 3 from restaurant 101
	http_response = requests.post(
		"http://localhost:8081/agentSignOut", json={"agentId": 202})

	result["4"] = http_response


def test():

	result = {}

	# Reinitialize Restaurant service
	http_response = requests.post("http://localhost:8080/reInitialize")

	# Reinitialize Delivery service
	http_response = requests.post("http://localhost:8081/reInitialize")

	# Reinitialize Wallet service
	http_response = requests.post("http://localhost:8082/reInitialize")

	# Place one new valid order 
	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 2, "qty": 4})
	
	if(http_response.status_code != HTTPStatus.CREATED):
		return 'Fail1'

	order_id1 = http_response.json().get("orderId")

	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 302, "restId": 101, "itemId": 2, "qty": 4})
		
	if(http_response.status_code != HTTPStatus.CREATED):
		return 'Fail2'
	
	order_id2 = http_response.json().get("orderId")

	time.sleep(5)


	### Parallel Execution Begins ###
	thread1 = Thread(target=t1, kwargs={"result": result})
	thread2 = Thread(target=t2, kwargs={"result": result})


	thread1.start()
	thread2.start()

	thread1.join()
	thread2.join()

	### Parallel Execution Ends ###
	status_code1 = result["1"].status_code
	status_code2 = result["2"].status_code
	
	time.sleep(5)
	
	if(status_code1 == HTTPStatus.CREATED and status_code2 == HTTPStatus.CREATED):

		http_response1 = requests.get(
		f"http://localhost:8081/agent/201")

		http_response2 = requests.get(
		f"http://localhost:8081/agent/202")

		status1 = http_response1.json().get("status")
		status2 = http_response2.json().get("status")

		if(not(status1 == "unavailable" and status2 == "unavailable")):
			return 'Fail3'

		thread3 = Thread(target=t3, kwargs={"result": result})
		thread4 = Thread(target=t4, kwargs={"result": result})


		thread3.start()
		thread4.start()

		thread3.join()
		thread4.join()

		### Parallel Execution Ends ###
		status_code3 = result["3"].status_code
		status_code4 = result["4"].status_code	

		time.sleep(5)

		if(status_code3 != HTTPStatus.CREATED and status_code4!= HTTPStatus.CREATED):
			return 'Fail4'

		http_response1 = requests.get(
		f"http://localhost:8081/agent/201")
		
		if(http_response1.status_code != HTTPStatus.OK):
			return 'Fail'
			
		http_response2 = requests.get(
		f"http://localhost:8081/agent/202")
		
		if(http_response2.status_code != HTTPStatus.OK):
			return 'Fail'
		
		status3 = http_response1.json().get("status")
		status4 = http_response2.json().get("status")

		if(not(status1==status3 and status2==status4)):
			return 'Fail5'

		http_response = requests.post("http://localhost:8081/orderDelivered", json={"orderId":str(order_id1)})
		
		if(http_response.status_code != HTTPStatus.CREATED):
			return 'Fail'
			
		http_response = requests.post("http://localhost:8081/orderDelivered", json={"orderId":str(order_id2)})
		
		if(http_response.status_code != HTTPStatus.CREATED):
			return 'Fail'
		
		time.sleep(10)
		http_response1 = requests.get(
		f"http://localhost:8081/agent/201")

		http_response2 = requests.get(
		f"http://localhost:8081/agent/202")

		status3 = http_response1.json().get("status")
		status4 = http_response2.json().get("status")
		
		#print(status1, status2, status3, status4)
		if(not(status1!=status3 and status2!=status4)):
			return 'Fail6'
			
		
		http_response = requests.post("http://localhost:8081/agentSignOut", json={"agentId":201})
		
		if(http_response.status_code != HTTPStatus.CREATED):
			return 'Fail'
			
		http_response = requests.post("http://localhost:8081/agentSignOut", json={"agentId":202})
		
		if(http_response.status_code != HTTPStatus.CREATED):
			return 'Fail'
			
		time.sleep(5)
		
		http_response1 = requests.get(
		f"http://localhost:8081/agent/201")

		http_response2 = requests.get(
		f"http://localhost:8081/agent/202")

		status5 = http_response1.json().get("status")
		status6 = http_response2.json().get("status")
		
		if(status5!="signed-out" or status6!="signed-out"):
			return 'Fail7'
		
	else:
		return 'Fail'
		
	return 'Pass'

	
if __name__ == "__main__":
	print(test())
