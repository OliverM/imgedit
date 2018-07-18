(ns imgedit.implementation
  "Implementation namespace. Do not use directly.")

(defn in-bounds?
  "Is the supplied dimension in bounds of the supplied image?"
  [{:keys [image/width image/height]} [dimension & value]]
  (case dimension
    :X (<= 1 (first value) width)
    :XS (let [[x1 x2] value] (and (<= 1 x1 width) (<= 1 x2 width)))
    :Y (<= 1 (first value) height)
    :YS (let [[y1 y2] value] (and (<= 1 y1 height) (<= 1 y2 height)))))

(defn command-in-bounds?
  "Check if the supplied conformed command is in bounds of the supplied image.
  Leverages the structure of pixel-oriented commands in that the first two
  command arguments are always the positional arguments. Commands without pixel
  arguments are within bounds by default."
  [image command]
  (let [[_ [_ [command-type & command-args]]] command]
    (if (#{:PIXEL :VERTICAL :HORIZONTAL :FILL} command-type)
      (and
        (in-bounds? image (first command-args))
        (in-bounds? image (second command-args)))
      true)))

(defn every-pixel-in-bounds?
  "Test an image's pixel entries are all within its bounds. Primarily useful as
  part of a spec generator for images."
  [{:keys [image/pixels] :as image}]
  (every? (fn [[[x y] _]]
            (and
              (in-bounds? image [:X x])
              (in-bounds? image [:Y y])))
    pixels))

(defn new-image
  "Create a new image from the supplied dimensions. Uses a sparse map
  representation of pixels."
  [width height]
  #:image{:width width :height height :pixels {}})

(defn clear
  "Given an image, returns an image with the pixel information removed."
  [image]
  (assoc image :image/pixels {}))

(defn get-colour
  "Return the colour at the supplied co-ordinates in the supplied image. Returns
  the default value of 'O' if no pixel has been stored at that location so far."
  [{:keys [image/pixels]} [_ x] [_ y]]
  (get pixels [x y] \O))

(defn pixel
  "Given an image and a set of pixel dimensions, returns an image with that pixel
  added to the image. This is the main means of adding pixel information to an
  image; other pixel-adding functions ultimately invoke this function."
  [image [_ x] [_ y] [_ c]]
  (assoc-in image [:image/pixels [x y]] c))

(defn vertical-line
  "Given an image and a set of vertical line dimensions, returns an image with
  that line added to the image."
  [image [_ x] [_ y1 y2] c]
  (let [pixels (for [y (range (min y1 y2) (inc (max y1 y2)))]
                 [[:X x] [:Y y] c])]
    (reduce #(apply pixel %1 %2) image pixels)))

(defn horizontal-line
  "Given an image and a set of horizontal line dimensions, returns an image with
  that line added to the image."
  [image [_ x1 x2] [_ y] c]
  (let [pixels (for [x (range (min x1 x2) (inc (max x1 x2)))]
                 [[:X x] [:Y y] c])]
    (reduce #(apply pixel %1 %2) image pixels)))

(defn matching-neighbours
  "Given an image, an [x, y] co-ordinate and a colour, return the neighbours of
  that point matching that colour. Neighbours are contiguous (they share a
  side). When a pixel is not present in the sparse pixel representation the
  default colour of 'O' is used."
  [{:keys [image/width image/height] :as image} [x y] c]
  (for [dx [(dec x) x (inc x)]
        dy [(dec y) y (inc y)]
        :when (and
                (<= 1 dx width) ;; check candidate in bounds of image...
                (<= 1 dy height)
                (or (= x dx) (= y dy)) ;; and not corner pixel to pixel
                (not (and (= x dx) (= y dy))) ;; and not same pixel
                (= c (get-colour image [:X dx] [:Y dy])))] ;; and has same colour
    [dx dy]))

(defn fill [image [_ x :as x-coord] [_ y :as y-coord] c]
  "Given an image, pixel co-ordinates and a colour, colours the pixel at that
  location that colour, as well as all contiguous pixels sharing the original
  pixel's colour. Contiguous pixels share sides (i.e. diagonally-touching pixels
  are excluded). When no pixel at the queried location is found, the default
  colour of 'O' is used."
  (let [source-colour (get-colour image x-coord y-coord)]
    (loop [image image
           candidates [[x y]]]
      (if (empty? candidates)
        image
        (let [[cx cy] (first candidates)
              updated-image (pixel image [:X cx] [:Y cy] c)]
          (recur
            updated-image
            (into (next candidates)
              (matching-neighbours updated-image [cx cy] source-colour))))))))

(defn show
  "Display an image via stdout. Transforms the sparse map representation of pixels
  into an array of characters suitable for display, printing it by side-effect.
  When no pixel data is found at a location, the default colour of 'O' is used.
  Returns the image."
  [{:keys [image/width image/height] :as image}]
  (let [image-lines (partition width
                      (for [y (range 1 (inc height))
                            x (range 1 (inc width))]
                        (get-colour image [:X x] [:Y y])))]
    (doseq [line image-lines] (run! print line) (newline)))
  image)

(defn quit
  []
  (System/exit 0))
