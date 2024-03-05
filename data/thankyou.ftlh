<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <title>Thank you!!</title>
    <link rel="stylesheet" href="css/candidates.css">
</head>

<body>
    <div class="container flex flex-col align-center">
        <h1>Thank you for your vote!</h1>
        <main class="align-center">
            <div class="card">
                <div class="flex flex-col align-center" href="#">
                    <img src="/images/${selectedCandidate.photo}">
                    <p>Thank you for your vote for ${selectedCandidate.name}!</p>
                    <p>${(selectedCandidate.getAmountOfVotes() / totalVotes * 100)?string("0.##")} % of all votes</p>
                </div>
            </div>
            <a class="back flex align-center" href="/">Back to main</a>

            <p>-------------------------------------------------------</p>
            <a class="back flex align-center" href="/votes">Show total votes</a>
        </main>
    </div>
</body>

</html>
